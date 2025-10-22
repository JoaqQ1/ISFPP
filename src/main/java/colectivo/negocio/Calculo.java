package colectivo.negocio;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import colectivo.util.Util;



/**
 * Clase Calculo
 * ------------------------
 * Contiene métodos para calcular recorridos entre dos paradas,
 * determinar la hora de salida adecuada según la hora de llegada del pasajero
 * y estimar la duración del viaje.
 */
public class Calculo {

    public Calculo(){}
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
    public List<List<Recorrido>> calcularRecorrido(
            Parada paradaOrigen,
            Parada paradaDestino,
            int diaSemana,
            LocalTime horaLlegaParada,
            Map<String, Tramo> tramos) {

        // ? ========== Recorridos Directos ==========√ 
        List<List<Recorrido>> listaRecorridos = new ArrayList<>();
        boolean recorridoDirectoEncontrado = false;

        // Iteramos sobre las líneas que pasan por la parada de origen
        for (Linea l1 : paradaOrigen.getLineas()) {
            // Si la línea también pasa por la parada destino Y el índice del destino es mayor
            // que el índice del origen (es decir, la dirección es correcta)
            List<Parada> paradasLinea = l1.getParadas();
            int idxOrigen = paradasLinea.indexOf(paradaOrigen);
            int idxDestino = paradasLinea.indexOf(paradaDestino);

            if (idxDestino > idxOrigen) {
                Recorrido recorrido = crearRecorrido(l1, paradaOrigen, paradaDestino, tramos, diaSemana, horaLlegaParada);

                // Solo se agrega si el recorrido es válido
                if (recorrido != null) {
                    List<Recorrido> recorridos = new ArrayList<>();
                    recorridos.add(recorrido);
                    listaRecorridos.add(recorridos);
                    recorridoDirectoEncontrado = true;
                }
            }

        }
        //? ========== Recorridos con Conexiones ==========
        if(!recorridoDirectoEncontrado)
            buscarConexiones(paradaOrigen, paradaDestino, diaSemana, horaLlegaParada, tramos, listaRecorridos);
        return listaRecorridos;
    }

    private void buscarConexiones(
        Parada origen,
        Parada destino,
        int diaSemana,
        LocalTime horaLlegada,
        Map<String, Tramo> tramos,
        List<List<Recorrido>> resultados) {

        boolean recorridoEncontrado = false;
        for (Linea l1 : origen.getLineas()) {
            List<Parada> paradasL1 = l1.getParadas();
            int idxOrigenEnL1 = paradasL1.indexOf(origen);
            // Recorremos cada parada de la primera línea como posible punto de transbordo
            for (Parada intermedia : paradasL1) {
                int idxIntermediaEnL1 = paradasL1.indexOf(intermedia);

                // la intermedia debe venir después del origen
                if (idxIntermediaEnL1 > idxOrigenEnL1) {

                    // Buscamos una segunda línea que pase por la parada intermedia y llegue al destino
                    for (Linea l2 : intermedia.getLineas()) {
                        List<Parada> paradasL2 = l2.getParadas();
                        int idxIntermediaEnL2 = paradasL2.indexOf(intermedia);
                        int idxDestinoEnL2 = paradasL2.indexOf(destino);

                        // Debe tener ambas paradas y el destino debe venir después de la intermedia
                        if (idxDestinoEnL2 > idxIntermediaEnL2) {

                            // Crear primer recorrido (origen -> intermedia)
                            Recorrido r1 = crearRecorrido(l1, origen, intermedia, tramos, diaSemana, horaLlegada);

                            if (r1 != null) {
                                // Calcular hora en la que se llega a la intermedia
                                LocalTime llegadaIntermedia = r1.getHoraSalida().plusSeconds(r1.getDuracion());

                                // Crear segundo recorrido (intermedia -> destino)
                                Recorrido r2 = crearRecorrido(l2, intermedia, destino, tramos, diaSemana, llegadaIntermedia);

                                if (r2 != null) {
                                    List<Recorrido> combinacion = new ArrayList<>();
                                    combinacion.add(r1);
                                    combinacion.add(r2);
                                    resultados.add(combinacion);
                                    recorridoEncontrado = true;
                                }
                            }
                        }
                    }
                }
                if(recorridoEncontrado){
                    recorridoEncontrado = false;
                    break;
                }
            }
            
        }
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
    private Recorrido crearRecorrido(
            Linea linea,
            Parada origen,
            Parada destino,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegadaParada) {

        if(origen.equals(destino)) return null;

        List<Parada> paradasRecorridas = new ArrayList<>();
        int duracionViaje = 0;
        boolean enTramo = false;

        Iterator<Parada> i = linea.getParadas().iterator();
        if (!i.hasNext()) return null;

        Parada anterior = i.next();

        while (i.hasNext()) {
            Parada actual = i.next();

            Tramo t = tramos.get(Util.claveTramo(anterior, actual));
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
        
        // No hay frecuencias para el horario de llegada
        if(horaSalida == null) return null;

        // Crear objeto Recorrido con todos los datos calculados
        return new Recorrido(linea, paradasRecorridas, horaSalida, duracionViaje);
    }

	/**
     * Calcula la hora de salida más próxima que permite llegar a la parada de origen
     * en o después de la hora de llegada del pasajero.
     */
    private LocalTime calcularHoraSalida(
            Linea linea,
            Parada origen,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegaParada) {

        int tiempoDesdeInicio = calcularTiempoDesdeInicio( origen, linea, tramos );
        // Recorremos los horarios de salida de la línea para ese día
        for (LocalTime horario : linea.getFrecuencias(diaSemana)) {
            LocalTime horaPasoPorOrigen = horario.plusSeconds(tiempoDesdeInicio);
            if (!horaPasoPorOrigen.isBefore(horaLlegaParada)) {
                return horaPasoPorOrigen; // Este es el horario de salida desde el inicio de línea
            }
        }

        // Si no hay frecuencias posteriores, devolvemos null
        return null;
    }
    /**
     * Calcula el tiempo total en segundos desde el inicio de la línea hasta la parada de origen.
     * Se utiliza para determinar la hora en la que un colectivo pasa por dicha parada.
     *
     * @param origen  Parada desde donde se inicia el cálculo
     * @param linea         Línea a evaluar
     * @param tramos        Mapa de tramos (clave: "codigoOrigen-codigoDestino")
     * @return Tiempo acumulado en segundos hasta la parada de origen
     */
    private int calcularTiempoDesdeInicio(
        Parada destino,
        Linea linea,
        Map<String, Tramo> tramos) {
        List<Parada> paradas = linea.getParadas();
        int tiempoAcumulado = 0;

        for (int i = 0; i < paradas.size() - 1; i++) {
            Parada actual = paradas.get(i);
            Parada siguiente = paradas.get(i + 1);
            
            // Si la parada actual es igual a la de destino no se debe acumular ningun segundo
            if (actual.equals(destino)) break;

            tiempoAcumulado += tramos.get(Util.claveTramo(actual, siguiente)).getTiempo();
            
            // Si la parada siguiente es igual a la de destino, no se debe acumular mas segundos
            if(siguiente.equals(destino)) break;
        }

        return tiempoAcumulado;
    }


}
