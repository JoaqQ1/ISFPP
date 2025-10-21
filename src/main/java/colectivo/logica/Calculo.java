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
 * ---------------------------------
 * Contiene la lógica principal para calcular recorridos posibles entre paradas,
 * considerando recorridos directos y con conexiones, además de determinar las horas
 * de salida más cercanas según frecuencias y hora de llegada del pasajero.
 */
public class Calculo {

    // ==============================
    // MÉTODO PRINCIPAL DE CÁLCULO
    // ==============================

    /**
     * Calcula todos los recorridos posibles entre una parada de origen y una parada destino.
     * 
     * - Primero busca recorridos directos (sin transbordos).
     * - Si no los hay, intenta construir recorridos con una conexión intermedia.
     *
     * @param paradaOrigen     Parada donde inicia el viaje
     * @param paradaDestino    Parada donde termina el viaje
     * @param diaSemana        Día de la semana (para determinar frecuencias)
     * @param horaLlegaParada  Hora en que el pasajero llega a la parada
     * @param tramos           Mapa de tramos (clave: "codigoOrigen-codigoDestino")
     * @return Lista de listas de recorridos posibles (cada lista puede contener uno o dos recorridos)
     */
    public static List<List<Recorrido>> calcularRecorrido(
            Parada paradaOrigen,
            Parada paradaDestino,
            int diaSemana,
            LocalTime horaLlegaParada,
            Map<String, Tramo> tramos) {

        // ---------- Recorridos Directos ----------
        List<List<Recorrido>> recorridosPosibles = new ArrayList<>();
        boolean existeDirecto = false;

        // Se evalúan todas las líneas que pasan por la parada de origen
        for (Linea linea : paradaOrigen.getLineas()) {

            // Si la línea también pasa por el destino → recorrido directo posible
            if (linea.getParadas().contains(paradaDestino)) {

                Recorrido recorrido = crearRecorrido(
                        linea,
                        paradaOrigen,
                        paradaDestino,
                        tramos,
                        diaSemana,
                        horaLlegaParada);

                // Solo se agrega si el recorrido es válido (no nulo)
                if (recorrido != null) {
                    List<Recorrido> recorridos = new ArrayList<>();
                    recorridos.add(recorrido);
                    recorridosPosibles.add(recorridos);
                    existeDirecto = true;
                }
            }
        }

        // ---------- Recorridos con Conexión ----------
        if (!existeDirecto) {
            buscarConexiones(paradaOrigen, paradaDestino, diaSemana, horaLlegaParada, tramos, recorridosPosibles);
        }

        return recorridosPosibles;
    }

    // ==============================
    // BÚSQUEDA DE CONEXIONES
    // ==============================

    /**
     * Busca recorridos con una conexión intermedia entre líneas distintas.
     * 
     * Ejemplo: Línea A lleva de origen a parada intermedia, y Línea B conecta desde esa
     * parada intermedia hasta el destino.
     */
    private static void buscarConexiones(
            Parada origen,
            Parada destino,
            int diaSemana,
            LocalTime horaLlegada,
            Map<String, Tramo> tramos,
            List<List<Recorrido>> resultados) {

        for (Linea primeraLinea : origen.getLineas()) {
            int indexOrigen = primeraLinea.getParadas().indexOf(origen);
            List<Parada> paradasLinea1 = primeraLinea.getParadas().subList(indexOrigen + 1, primeraLinea.getParadas().size());

            // Cada parada posterior al origen es candidata a ser punto de transbordo
            for (Parada paradaConexion : paradasLinea1) {
                boolean yaUsada = false;

                // Primer tramo del viaje (origen → conexión)
                Recorrido recorrido1 = crearRecorrido(primeraLinea, origen, paradaConexion, tramos, diaSemana, horaLlegada);

                // Buscamos una segunda línea que conecte la parada intermedia con el destino
                for (Linea segundaLinea : paradaConexion.getLineas()) {
                    if (!segundaLinea.equals(primeraLinea)) {
                        if (segundaLinea.getParadas().contains(destino)) {
                            int indexIntermedia = segundaLinea.getParadas().indexOf(paradaConexion);
                            int indexDestino = segundaLinea.getParadas().indexOf(destino);

                            // Verifica que el destino esté después de la parada de conexión
                            if (indexIntermedia < indexDestino) {
                                // El segundo tramo comienza al llegar al punto de conexión
                                LocalTime horaInicioSegundaParte = recorrido1.getHoraLlegada();
                                Recorrido recorrido2 = crearRecorrido(
                                        segundaLinea,
                                        paradaConexion,
                                        destino,
                                        tramos,
                                        diaSemana,
                                        horaInicioSegundaParte);

                                if (recorrido2 != null) {
                                    List<Recorrido> combinacion = new ArrayList<>();
                                    combinacion.add(recorrido1);
                                    combinacion.add(recorrido2);
                                    resultados.add(combinacion);
                                    yaUsada = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (yaUsada) break;
            }
        }
    }

    // ==============================
    // CREACIÓN DE UN RECORRIDO
    // ==============================

    /**
     * Crea un recorrido dentro de una misma línea, desde una parada origen hasta una destino.
     * Calcula la duración total, las paradas recorridas y las horas de salida.
     */
    private static Recorrido crearRecorrido(
            Linea linea,
            Parada origen,
            Parada destino,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegadaParada) {

        List<Parada> paradasRecorridas = new ArrayList<>();
        int tiempoTotalViaje = 0;
        boolean inicioDetectado = false;

        Iterator<Parada> i = linea.getParadas().iterator();
        if (!i.hasNext()) return null;

        Parada anterior = i.next();

        while (i.hasNext()) {
            Parada actual = i.next();

            Tramo tramoActual = tramos.get(generarClaveTramo(anterior, actual));

            // Se detecta el inicio del recorrido
            if (anterior.equals(origen)) {
                inicioDetectado = true;
                paradasRecorridas.add(origen);
            }

            // Si ya estamos dentro del recorrido, acumulamos
            if (inicioDetectado) {
                tiempoTotalViaje += tramoActual.getTiempo();
                paradasRecorridas.add(actual);
            }

            // Llegamos al destino → fin del recorrido
            if (actual.equals(destino)) break;

            anterior = actual;
        }

        // No se encontró conexión válida
        if (paradasRecorridas.isEmpty()) return null;

        // Determina la hora de salida en base a frecuencias
        LocalTime horaSalida = calcularHoraSalida(linea, origen, tramos, diaSemana, horaLlegadaParada);
        if (horaSalida == null) return null;

        // Hora estimada en la que el colectivo pasa por el origen
        LocalTime horaSalidaOrigen = horaSalida.plusSeconds(calcularTiempoDesdeInicio(origen, linea, tramos));

        return new Recorrido(linea, paradasRecorridas, horaSalidaOrigen, horaSalida, tiempoTotalViaje);
    }

    // ==============================
    // CÁLCULO DE HORARIOS
    // ==============================

    /**
     * Busca la primera frecuencia cuyo paso por la parada origen sea
     * igual o posterior a la hora en que el pasajero llega.
     */
    private static LocalTime calcularHoraSalida(
            Linea linea,
            Parada origen,
            Map<String, Tramo> tramos,
            int diaSemana,
            LocalTime horaLlegadaParada) {

        int tiempoDesdeInicio = calcularTiempoDesdeInicio(origen, linea, tramos);

        for (LocalTime horario : linea.getFrecuencias(diaSemana)) {
            LocalTime horaPasoPorOrigen = horario.plusSeconds(tiempoDesdeInicio);
            if (!horaPasoPorOrigen.isBefore(horaLlegadaParada)) {
                return horario; // hora de salida de la linea
            }
        }
        return null; // no hay frecuencias posteriores
    }

    // ==============================
    // TIEMPOS Y TRAMOS
    // ==============================

    /**
     * Calcula el tiempo acumulado (en segundos) desde el inicio de la línea
     * hasta la parada especificada.
     */
    private static int calcularTiempoDesdeInicio(
            Parada destino,
            Linea linea,
            Map<String, Tramo> tramos) {

        List<Parada> paradas = linea.getParadas();
        int tiempoAcumulado = 0;

        for (int i = 0; i < paradas.size() - 1; i++) {
            Parada actual = paradas.get(i);
            Parada siguiente = paradas.get(i + 1);

            if (actual.equals(destino)) break;

            tiempoAcumulado += tramos.get(generarClaveTramo(actual, siguiente)).getTiempo();

            if (siguiente.equals(destino)) break;
        }

        return tiempoAcumulado;
    }

    /**
     * Genera la clave estándar para identificar un tramo dentro del mapa de tramos.
     * 
     * Ejemplo: "12-15" (donde 12 = código origen, 15 = código destino)
     */
    private static String generarClaveTramo(Parada origen, Parada destino) {
        return String.format("%d-%d", origen.getCodigo(), destino.getCodigo());
    }
}
