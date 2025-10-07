package colectivo.datos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

/**
 * La clase {@code CargarDatos} provee métodos utilitarios para cargar
 * información de archivos de texto y transformarla en objetos de dominio
 * como {@link Parada}, {@link Tramo} y {@link Linea}.
 *
 * Los archivos deben tener un formato delimitado por punto y coma (;).
 */
public class CargarDatos {

    /**
     * Carga todas las paradas desde un archivo.
     * <p>
     * Formato esperado de cada línea del archivo:
     * <pre>
     * codParada;direccion;latitud;longitud
     * </pre>
     * Ejemplo:
     * <pre>
     * 1;1 De Marzo, 405;-42.766285;-65.040768
     * </pre>
     *
     * @param nombreArchivo nombre del archivo de recursos (en classpath)
     * @return un {@link Map} de paradas indexadas por su código
     * @throws IOException si no se encuentra el archivo o ocurre un error de lectura
     */
    public static Map<Integer, Parada> cargarParadas(String nombreArchivo) throws IOException {
        TreeMap<Integer, Parada> paradas = new TreeMap<>();
        InputStream datosParadas = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        Scanner read = new Scanner(datosParadas);
        read.useDelimiter("\\s*;\\s*"); // separador: punto y coma con espacios opcionales

        while (read.hasNext()) {
            int codParada = read.nextInt();
            String direccion = read.next();
            double latitud = Double.parseDouble(read.next());
            double longitud = Double.parseDouble(read.next());

            // Se agrega la parada al mapa
            paradas.put(codParada, new Parada(codParada, direccion, latitud, longitud));
        }
        read.close();
        return paradas;
    }

    /**
     * Carga todos los tramos desde un archivo.
     * <p>
     * Formato esperado de cada línea:
     * <pre>
     * codParadaInicio;codParadaFin;tiempo;tipo
     * </pre>
     *
     * @param nombreArchivo nombre del archivo de recursos (en classpath)
     * @param paradas       mapa de paradas ya cargadas (para asociar inicios y finales)
     * @return un {@link Map} de tramos indexados por la dirección de la parada de inicio
     * @throws FileNotFoundException si no se encuentra el archivo
     */
    public static Map<String, Tramo> cargarTramos(String nombreArchivo, Map<Integer, Parada> paradas)
            throws FileNotFoundException {

        TreeMap<String, Tramo> tramos = new TreeMap<>();

        InputStream datosTramos = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        Scanner read = new Scanner(datosTramos);
        read.useDelimiter("\\s*;\\s*");

        while (read.hasNext()) {
            Parada inicio = paradas.get(read.nextInt());
            Parada fin = paradas.get(read.nextInt());
            int tiempo = read.nextInt();
            int tipo = read.nextInt();

            // Se indexa por la dirección de la parada de inicio
            tramos.put(
                String.format("%d-%d", inicio.getCodigo(),fin.getCodigo()), new Tramo(inicio, fin, tiempo, tipo));
        }
        read.close();
        return tramos;
    }

    /**
     * Carga todas las líneas de colectivo desde un archivo.
     * <p>
     * Formato esperado:
     * <pre>
     * codLinea;nombreLinea;codParada1;codParada2;...;codParadaN
     * </pre>
     * Además, llama a {@link #agregarFrecuencias(String, Map)} para cargar las frecuencia.
     *
     * @param nombreArchivo          archivo con líneas y sus paradas
     * @param nombreArchivoFrecuencia archivo con frecuencias de cada línea
     * @param paradas                mapa de paradas ya cargadas
     * @return un {@link Map} de líneas indexadas por código
     * @throws FileNotFoundException si no se encuentran los archivos
     */
    public static Map<String, Linea> cargarLineas(String nombreArchivo, String nombreArchivoFrecuencia,
            Map<Integer, Parada> paradas) throws FileNotFoundException {

        if(nombreArchivo.isEmpty() || nombreArchivoFrecuencia.isEmpty() || paradas == null) throw new IllegalArgumentException("Error al leer archivos de lineas");
        TreeMap<String, Linea> lineas = new TreeMap<>();
        InputStream datosLineas = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivo);
        Scanner read = new Scanner(datosLineas);

        while (read.hasNextLine()) {
            String line = read.nextLine();
            Scanner readLine = new Scanner(line);
            readLine.useDelimiter("\\s*;\\s*");

            String codLinea = readLine.next();
            String nombreLinea = readLine.next();
            Linea linea = new Linea(codLinea, nombreLinea);

            // Agregar todas las paradas asociadas a la línea
            while (readLine.hasNext()) {
                linea.agregarParada(paradas.get(readLine.nextInt()));
            }
            lineas.put(codLinea, linea);
            readLine.close();
        }

        // Agregar frecuencias de circulación
        agregarFrecuencias(nombreArchivoFrecuencia, lineas);
        read.close();
        return lineas;
    }

    /**
     * Método auxiliar para cargar las frecuencias (horarios) de cada línea.
     * <p>
     * Formato esperado:
     * <pre>
     * codLinea;dia;hora
     * </pre>
     * Ejemplo:
     * <pre>
     * 101;1;06:30
     * </pre>
     *
     * @param nombreArchivoFrecuencia archivo de frecuencias
     * @param lineas                  mapa de líneas a las que se les agregarán frecuencias
     */
    private static void agregarFrecuencias(String nombreArchivoFrecuencia, Map<String, Linea> lineas) {
        
        if(nombreArchivoFrecuencia.isEmpty() || lineas == null || lineas.isEmpty()) 
            throw new IllegalArgumentException("Error al leer archivos de frecuencias.");

        InputStream datosLineas = CargarDatos.class.getClassLoader().getResourceAsStream(nombreArchivoFrecuencia);
        Scanner read = new Scanner(datosLineas);
        read.useDelimiter("\\s*;\\s*");

        while (read.hasNext()) {
            Linea linea = lineas.get(read.next());
            int dia = read.nextInt();
            LocalTime hora = LocalTime.parse(read.next());

            // Se agrega la frecuencia a la línea correspondiente
            linea.agregarFrecuencia(dia, hora);
        }
        read.close();
    }
}
