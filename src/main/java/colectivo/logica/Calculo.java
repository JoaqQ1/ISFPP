package colectivo.logica;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

        // ? ========== Recorridos Directos ==========√ 
        List<List<Recorrido>> listaRecorridos = new ArrayList<>();

        // Iteramos sobre las líneas que pasan por la parada de origen
        for (Linea l1 : paradaOrigen.getLineas()) {

            // Si la línea también pasa por la parada destino, se calcula el recorrido
            if (l1.getParadas().contains(paradaDestino)) {

                Recorrido recorrido = crearRecorrido(l1, paradaOrigen, paradaDestino, tramos, diaSemana, horaLlegaParada);

                // Solo se agrega si el recorrido es válido
                if (recorrido != null) {
                    List<Recorrido> recorridos = new ArrayList<>();
                    recorridos.add(recorrido);
                    listaRecorridos.add(recorridos);
                }
            }

        }
        //! ========== Recorridos con Conexiones ==========
        buscarConexiones(paradaOrigen, paradaDestino, diaSemana, horaLlegaParada, tramos, listaRecorridos);
        return listaRecorridos;
    }

    private static void buscarConexiones(
            Parada origen,
            Parada destino,
            int diaSemana,
            LocalTime horaLlegada,
            Map<String, Tramo> tramos,
            List<List<Recorrido>> resultados) {

        for (Linea l1 : origen.getLineas()) {

            // Recorremos cada parada de la primera línea como posible punto de transbordo
            for (Parada intermedia : l1.getParadas()) {
                if (!intermedia.equals(origen)){

                    Recorrido r1 = crearRecorrido(l1, origen, intermedia, tramos, diaSemana, horaLlegada);
                    // if (r1 == null) continue;
    
                    // Ahora buscamos una segunda línea que pase por la parada intermedia y llegue al destino
                    for (Linea l2 : intermedia.getLineas()) {
                        if (l2.equals(l1)) continue; // No repetir misma línea
                        if (!l2.getParadas().contains(destino)) continue;
    
                        int indexIntermedia = l2.getParadas().indexOf(intermedia);
                        int indexDestino = l2.getParadas().indexOf(destino);
                        if (indexIntermedia == -1 || indexDestino == -1 || indexIntermedia >= indexDestino) continue;
    
                        // La segunda parte del viaje parte después de llegar a la parada intermedia
                        LocalTime horaInicioSegundaParte = r1.getHoraSalida();
                        Recorrido r2 = crearRecorrido(l2, intermedia, destino, tramos, diaSemana, horaInicioSegundaParte);
    
                        if (r2 != null) {
                            List<Recorrido> combinacion = new ArrayList<>();
                            combinacion.add(r1);
                            combinacion.add(r2);
                            resultados.add(combinacion);
                            break;
                        }
                    }
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

            Tramo t = tramos.get(claveTramo(anterior, actual));
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
    private static LocalTime calcularHoraSalida(
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
    private static int calcularTiempoDesdeInicio(
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

            tiempoAcumulado += tramos.get(claveTramo(actual, siguiente)).getTiempo();
            
            // Si la parada siguiente es igual a la de destino, no se debe acumular mas segundos
            if(siguiente.equals(destino)) break;
        }

        return tiempoAcumulado;
    }

    /**
     * Devuelve un String pre-formateado como la key del mapa de tramos 
     * @param origen Origen del tramo a buscar
     * @param destino Destino del tramo a buscar
     * @return  Retorna un String pre-formateado con los codigos de origen y destino de la siguiente manera. "codigo_origen-codigo_destino"
     */
    private static String claveTramo(Parada origen,Parada destino){
        return String.format("%d-%d",origen.getCodigo(),destino.getCodigo());
    }
    
    
    private static List<Parada> obtenerTodasLasParadas(Map<String, Tramo> tramos) {
        // Usamos Set para evitar duplicados
        Set<Parada> paradas = new HashSet<>();
        for (Tramo t : tramos.values()) {
            paradas.add(t.getInicio());
            paradas.add(t.getFin());
        }
        return new ArrayList<>(paradas);
    }

    private static List<Linea> obtenerTodasLasLineas(Map<String, Tramo> tramos) {

        // Usamos Set para evitar duplicados
        Set<Linea> lineas = new HashSet<>();
        
        for(Parada p: obtenerTodasLasParadas(tramos)){
            for(Linea l:p.getLineas()){
                lineas.add(l);
            }
        }
        return new ArrayList<Linea>(lineas);
    }
    // private static Map<Parada, List<Tramo>> crearRedTramos(List<Parada> paradas, Map<String, Tramo> tramos) {
    //     Map<Parada, List<Tramo>> redDeTransporte = new HashMap<>();

    //     // Inicializamos la red
    //     for (Parada p : paradas) {
    //         redDeTransporte.put(p, new ArrayList<>());
    //     }

    //     // Conectamos cada tramo
    //     for (Tramo t : tramos.values()) {
    //         Parada inicio = t.getInicio();
    //         redDeTransporte.get(inicio).add(t);
    //     }

    //     return redDeTransporte;
    // }

    // private static List<Tramo> buscarRecorridos(
    //         Parada actual,
    //         Parada destino,
    //         Map<Parada,List<Tramo>> redDeTramos,
    //         Deque<Tramo> caminoActual,
    //         Set<Parada> visitadas) {

    //     if (actual.equals(destino)) {
    //         return new ArrayList<>(caminoActual); // copia defensiva
    //     }

    //     // Protecciones
    //     if (visitadas.contains(actual)) return null;
    //     visitadas.add(actual);

    //     List<Tramo> salidas = redDeTramos.get(actual);
    //     if (salidas != null) {
    //         for (Tramo tramo : salidas) {
    //             Parada siguiente = tramo.getFin();
    //             // evitar volver a entrar si ya visitada
    //             if (visitadas.contains(siguiente)) continue;

    //             caminoActual.addLast(tramo);
    //             List<Tramo> resultado = buscarRecorridos(siguiente, destino, redDeTramos, caminoActual, visitadas);
    //             if (resultado != null) {
    //                 return resultado; // corta y devuelve la primera ruta encontrada
    //             }
    //             caminoActual.removeLast(); // backtracking
    //         }
    //     }

    //     visitadas.remove(actual);
    //     return null;
    // }

}
