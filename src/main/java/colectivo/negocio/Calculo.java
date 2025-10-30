package colectivo.negocio;

import java.security.KeyStore.Entry;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        boolean recorridoConConexionEncontrado = false;

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
        if(!recorridoDirectoEncontrado){
            recorridoConConexionEncontrado = buscarConexiones(
                                                paradaOrigen, 
                                                paradaDestino, 
                                                diaSemana, 
                                                horaLlegaParada, 
                                                tramos, 
                                                listaRecorridos);
        }
        if(!recorridoConConexionEncontrado){
            
            buscarConexionesCaminando(
                                    paradaOrigen, 
                                    paradaDestino, 
                                    diaSemana, 
                                    horaLlegaParada, 
                                    tramos, 
                                    listaRecorridos);
        }
        return listaRecorridos;
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
    private boolean buscarConexiones(
        Parada origen,
        Parada destino,
        int diaSemana,
        LocalTime horaLlegada,
        Map<String, Tramo> tramos,
        List<List<Recorrido>> resultados) {
        boolean recorridoEncontrado = false;

        for (Linea primeraLinea : origen.getLineas()) {
            int indexOrigen = primeraLinea.getParadas().indexOf(origen);
            List<Parada> paradasLinea1 = primeraLinea.getParadas().subList(indexOrigen + 1, primeraLinea.getParadas().size());
            
            // Cada parada posterior al origen es candidata a ser punto de transbordo
            for (Parada paradaConexion : paradasLinea1) {
                boolean trasbordoEncontrado = false;

                // Primer tramo del viaje (origen → conexión)
                Recorrido recorrido1 = crearRecorrido(
                            primeraLinea,
                            origen, 
                            paradaConexion, 
                            tramos, 
                            diaSemana, 
                            horaLlegada);

                // Buscamos una segunda línea que conecte la parada intermedia con el destino
                for (Linea segundaLinea : paradaConexion.getLineas()) {
                    if (!segundaLinea.equals(primeraLinea)) {
                        if (segundaLinea.getParadas().contains(destino)) {
                            int indexIntermedia = segundaLinea.getParadas().indexOf(paradaConexion);
                            int indexDestino = segundaLinea.getParadas().indexOf(destino);

                            // Verifica que el destino esté después de la parada de conexión
                            if (indexIntermedia < indexDestino) {
                                // El segundo tramo comienza al llegar al punto de conexión
                                LocalTime horaInicioSegundaParte = recorrido1.getHoraSalida().plusSeconds(recorrido1.getDuracion());
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
                                    trasbordoEncontrado = true;
                                    recorridoEncontrado = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                // El primer trasbordo que se encuentra en el que se agrega
                if (trasbordoEncontrado) {
                    break;
                }
                

            }
        }

        return recorridoEncontrado;
    }

    public void buscarConexionesCaminando(
        Parada origen,
        Parada destino,
        int diaSemana,
        LocalTime horaLlegada,
        Map<String, Tramo> tramos,
        List<List<Recorrido>> resultados){
            for (Linea primeraLinea : origen.getLineas()) {
                int indexOrigen = primeraLinea.getParadas().indexOf(origen);
                List<Parada> paradasLinea1 = primeraLinea.getParadas().subList(indexOrigen + 1, primeraLinea.getParadas().size());

                // Cada parada posterior al origen es candidata a ser punto de transbordo
                for (Parada paradaConexion : paradasLinea1) {
                    boolean trasbordoEncontrado = false;

                    // Primer tramo del viaje (origen → conexión)
                    Recorrido recorrido1 = crearRecorrido(primeraLinea,origen, paradaConexion, tramos, diaSemana, horaLlegada);
                    System.out.println(paradaConexion.getParadaCaminando());
                    for(Parada paradaCaminando : paradaConexion.getParadaCaminando()){
                        for(Linea segundaLinea:paradaCaminando.getLineas()){
                            if(segundaLinea.getParadas().contains(destino)){
                                LocalTime horaInicioSegundaParte = recorrido1.getHoraSalida().plusSeconds(recorrido1.getDuracion());
                                Tramo t = tramos.get(Util.claveTramo(paradaConexion, paradaCaminando));
                                Recorrido recorrido2 = new Recorrido(null, List.of(t.getInicio(),t.getFin()), horaInicioSegundaParte, t.getTiempo());
                                int indexIntermedia = segundaLinea.getParadas().indexOf(t.getFin());
                                int indexDestino = segundaLinea.getParadas().indexOf(destino);
                                
                                if (indexIntermedia < indexDestino) {
                                    // El segundo tramo comienza al llegar al punto de conexión
                                    LocalTime horaInicioTerceraParte = recorrido2.getHoraSalida().plusSeconds(recorrido2.getDuracion());
                                    Recorrido recorrido3 = crearRecorrido( segundaLinea,t.getFin(),destino,tramos,diaSemana,horaInicioTerceraParte);

                                    
                                    if (recorrido2 != null) {
                                        List<Recorrido> combinacion = new ArrayList<>();
                                        combinacion.add(recorrido1);
                                        combinacion.add(recorrido2);
                                        combinacion.add(recorrido3);
                                        resultados.add(combinacion);
                                        trasbordoEncontrado = true;
                                        break;
                                    }   
                                }
                            }
                        }
                    }
                    if (trasbordoEncontrado) {
                        break;
                    }
                }
            }
        }
    // ==============================
    // CREACIÓN DE UN RECORRIDO
    // ==============================
    
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
		LocalTime horaSalida = obtenerProximaHoraDePaso(linea, origen, tramos, diaSemana, horaLlegadaParada);
        
        // No hay frecuencias para el horario de llegada
        if(horaSalida == null) return null;

        // Crear objeto Recorrido con todos los datos calculados
        return new Recorrido(linea, paradasRecorridas, horaSalida, duracionViaje);
    }

	/**
     * Obtiene la próxima hora en la que la línea pasa por una parada de origen,
     * considerando las frecuencias de salida desde el inicio de línea y el tiempo
     * de recorrido hasta dicha parada. Solo se devuelve una hora igual o posterior
     * a la hora en que el pasajero llega a la parada.
     *
     * @param linea            Línea a evaluar
     * @param origen           Parada en la que se encuentra el pasajero
     * @param tramos           Mapa de tramos (clave: "codigoOrigen-codigoDestino")
     * @param diaSemana        Día de la semana (para obtener las frecuencias correspondientes)
     * @param horaLlegaParada  Hora en que el pasajero llega a la parada
     * @return La próxima hora en que la línea pasa por la parada de origen,
     *         o {@code null} si no hay frecuencias disponibles después de la hora indicada
     */
    private LocalTime obtenerProximaHoraDePaso(
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
