package colectivo.logica;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;

/**
 * Clase Calculo
 * ------------------------
 * Contiene métodos para calcular recorridos entre dos paradas,
 * determinar la hora de salida adecuada según la hora de llegada del pasajero
 * y estimar la duración del viaje.
 */
public class Calculo {

    /**
     * Calcula todos los recorridos posibles entre una parada origen y una parada destino,
     * considerando las líneas que conectan ambas.
     *
     * @param paradaOrigen     Parada donde inicia el viaje
     * @param paradaDestino    Parada donde termina el viaje
     * @param diaSemana        Día de la semana (para determinar frecuencias)
     * @param horaLlegaParada  Hora en que el pasajero llega a la parada
     * @param tramos           Mapa de tramos (clave: "codigoOrigen-codigoDestino")
     * @return Lista de listas de recorridos posibles
     */
    public static List<List<Recorrido>> calcularRecorrido(
            Parada paradaOrigen,
            Parada paradaDestino,
            int diaSemana,
            LocalTime horaLlegaParada,
            Map<String, Tramo> tramos) {

        List<List<Recorrido>> listaRecorridos = new ArrayList<>();

        // Iteramos sobre las líneas que pasan por la parada de origen
        for (Linea linea : paradaOrigen.getLineas()) {

            // Si la línea también pasa por la parada destino, se calcula el recorrido
            if (linea.getParadas().contains(paradaDestino)) {

                Recorrido recorrido = crearRecorrido(linea, paradaOrigen, paradaDestino, tramos, diaSemana, horaLlegaParada);

                // Solo se agrega si el recorrido es válido
                if (recorrido != null) {
                    List<Recorrido> recorridos = new ArrayList<>();
                    recorridos.add(recorrido);
                    listaRecorridos.add(recorridos);
                }
            }
        }

        return listaRecorridos;
    }

    /**
     * Crea un recorrido entre una parada origen y una destino dentro de una línea,
     * considerando los tramos, las frecuencias y la hora de llegada del pasajero.
     *
     * @param linea             Línea a evaluar
     * @param origen            Parada de origen
     * @param destino           Parada de destino
     * @param tramos            Mapa de tramos
     * @param diaSemana         Día de la semana (para obtener frecuencias)
     * @param horaLLegadaParada Hora en que el pasajero llega a la parada
     * @return Objeto Recorrido con paradas, hora de salida y duración
     */
    private static Recorrido crearRecorrido(
            Linea linea,
            Parada origen,
            Parada destino,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegadaParada) {

        List<Parada> paradasRecorridas = new ArrayList<>();
        int duracionViaje = 0;
        boolean enTramo = false;

        Iterator<Parada> i = linea.getParadas().iterator();
        if (!i.hasNext()) return null;

        Parada anterior = i.next();

        while (i.hasNext()) {
            Parada actual = i.next();

            Tramo t = tramos.get(String.format("%d-%d", anterior.getCodigo(), actual.getCodigo()));
            if (t == null) {
                anterior = actual;
                continue;
            }

            // Activamos el tramo cuando llegamos a la parada de origen
            if (anterior.equals(origen)) {
                enTramo = true;
                paradasRecorridas.add(anterior); // se agrega solo una vez la parada de origen
            }

            // Si estamos en tramo, acumulamos duración y agregamos las paradas
            if (enTramo) {
                duracionViaje += t.getTiempo();
                paradasRecorridas.add(actual);
            }

            // Si llegamos al destino, terminamos el recorrido
            if (actual.equals(destino)) break;

            anterior = actual;
        }

        // Si no se encontró una conexión válida, no hay recorrido
        if (paradasRecorridas.isEmpty()) return null;

        // Calcular hora de salida según la frecuencia más próxima
		LocalTime horaSalida = calcularHoraSalida(linea, origen, tramos, diaSemana, horaLlegadaParada);
	

        // Crear objeto Recorrido con todos los datos calculados
        return new Recorrido(linea, paradasRecorridas, horaSalida, duracionViaje);
    }

	/**
     * Calcula la hora de salida más próxima que permite llegar a la parada de origen
     * en o después de la hora de llegada del pasajero.
     */
    private static LocalTime calcularHoraSalida(
            Linea linea,
            Parada paradaOrigen,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegaParada) {

        int tiempoDesdeInicio = calcularTiempoDesdeInicio(linea, paradaOrigen, tramos);

        // Recorremos los horarios de salida de la línea para ese día
        for (LocalTime horario : linea.getFrecuencias(diaSemana)) {
            LocalTime horaPasoPorOrigen = horario.plusSeconds(tiempoDesdeInicio);
            if (!horaPasoPorOrigen.isBefore(horaLlegaParada)) {
                return horaPasoPorOrigen; // Este es el horario de salida desde el inicio de línea
            }
        }

        // Si no hay frecuencias posteriores, devolvemos null (o podrías manejarlo distinto)
        return null;
    }
    /**
     * Calcula el tiempo total en segundos desde el inicio de la línea hasta la parada de origen.
     * Se utiliza para determinar la hora en la que un colectivo pasa por dicha parada.
     *
     * @param linea         Línea a evaluar
     * @param paradaOrigen  Parada desde donde se inicia el cálculo
     * @param tramos        Mapa de tramos (clave: "codigoOrigen-codigoDestino")
     * @return Tiempo acumulado en segundos hasta la parada de origen
     */
    private static int calcularTiempoDesdeInicio(Linea linea, Parada paradaOrigen, Map<String, Tramo> tramos) {
        List<Parada> paradas = linea.getParadas();
        int tiempoAcumulado = 0;

        for (int i = 0; i < paradas.size() - 1; i++) {
            Parada actual = paradas.get(i);
            Parada siguiente = paradas.get(i + 1);

            Tramo tramo = tramos.get(String.format("%d-%d", actual.getCodigo(), siguiente.getCodigo()));
            if (tramo == null) continue;

            if (siguiente.equals(paradaOrigen)) {
                tiempoAcumulado += tramo.getTiempo();
                break; // Llegamos a la parada de origen
            }

            tiempoAcumulado += tramo.getTiempo();
        }

        return tiempoAcumulado;
    }

}
