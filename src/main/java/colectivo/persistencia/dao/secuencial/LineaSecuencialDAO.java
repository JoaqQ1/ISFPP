package colectivo.persistencia.dao.secuencial;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.LineaDAO;
import colectivo.persistencia.dao.ParadaDAO;

public class LineaSecuencialDAO implements LineaDAO {

    private static final Logger LOGGER = LogManager.getLogger(LineaSecuencialDAO.class.getName());

    private Map<String, Linea> lineas;
    private String archivoLineas;
    private String archivoFrecuencias;
    private boolean actualizar;
    
    public LineaSecuencialDAO() {
        // 1. Obtener la configuración global
        ConfiguracionGlobal config = ConfiguracionGlobal.getConfiguracionGlobal();
        
        // 2. Obtener el código de la ciudad actual (ej: "CO")
        String ciudadActual = config.getCiudadActual();
        
        // 3. Leemos los nombres de archivos desde secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle(Constantes.PATH_DATA_TXT);
        
        // 4. Construir las claves dinámicamente
        String claveLinea = "linea." + ciudadActual;         // Ej: "linea.CO"
        String claveFrecuencia = "frecuencia." + ciudadActual; // Ej: "frecuencia.CO"

        // 5. Obtener los nombres de archivo usando las claves dinámicas
        archivoLineas = rb.getString(claveLinea);
        archivoFrecuencias = rb.getString(claveFrecuencia);
        
        LOGGER.info("LineaSecuencialDAO inicializado para la ciudad: " + ciudadActual);
        LOGGER.info("Usando archivos: " + archivoLineas + ", " + archivoFrecuencias);
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        if (lineas == null || actualizar) {
            lineas = readFromFile(archivoLineas, archivoFrecuencias);
            actualizar = false;
            LOGGER.info("Líneas cargadas desde archivos.");
        }
        return lineas;
    }

    public void insertar(Linea linea) {
        lineas.put(linea.getCodigo(), linea);
        writeToFile(lineas, archivoLineas, archivoFrecuencias);
        actualizar = true;
    }

    public void actualizar(Linea linea) {
        lineas.put(linea.getCodigo(), linea);
        writeToFile(lineas, archivoLineas, archivoFrecuencias);
        actualizar = true;
    }

    public void borrar(Linea linea) {
        lineas.remove(linea.getCodigo());
        writeToFile(lineas, archivoLineas, archivoFrecuencias);
        actualizar = true;
    }

    // ---------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------

    /**
     * Lee las líneas y sus frecuencias desde archivos.
     *
     * Formato archivo de líneas:
     * codLinea;nombreLinea;codParada1;codParada2;...;codParadaN
     *
     * Formato archivo de frecuencias:
     * codLinea;dia;hora
     */
    private Map<String, Linea> readFromFile(String archivoLineas, String archivoFrecuencias) {
        Map<String, Linea> map = new TreeMap<>();
        Scanner inFile = null;
        Map<Integer,Parada> paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();
        try {
            inFile = new Scanner(new File("src/main/resources/" + archivoLineas));
            while (inFile.hasNextLine()) {
                String line = inFile.nextLine();
                Scanner readLine = new Scanner(line);
                readLine.useDelimiter("\\s*;\\s*");
                String codLinea = readLine.next();
                String nombreLinea = readLine.next();
                
                Linea linea = new Linea(codLinea, nombreLinea);
                
                // Agregar paradas
                while (readLine.hasNextInt()) {
                    int codParada = readLine.nextInt();
                    Parada p = paradas.get(codParada);
                    if (p != null) linea.agregarParada(p);
                }
                map.put(codLinea, linea);
                readLine.close();
            }

            // Agregamos frecuencias desde el segundo archivo
            agregarFrecuencias(archivoFrecuencias, map);
            LOGGER.info("Frecuencias agregadas desde archivo: " + archivoFrecuencias);

        } catch (FileNotFoundException e) {
            LOGGER.error("readFromFile: Archivo no encontrado: " + archivoLineas, e);
        } catch (NoSuchElementException e) {
            LOGGER.error("readFromFile: Error en la estructura del archivo de líneas.", e);
        }
        catch(Exception e){
            LOGGER.error("readFromFile: Algo salio mal leyendo las lineas"+e.getMessage());
        } 
        finally {
            if (inFile != null)
                inFile.close();
            LOGGER.info("Lectura de líneas finalizada.");
        }
        return map;
    }

    /**
     * Lee las frecuencias (horarios) y las agrega a cada línea.
     */
    private void agregarFrecuencias(String archivoFrecuencias, Map<String, Linea> lineas) {
        Scanner inFile = null;
        try {
            inFile = new Scanner(new File("src/main/resources/" + archivoFrecuencias));
            inFile.useDelimiter("\\s*;\\s*");

            while (inFile.hasNext()) {
                String codLinea = inFile.next();
                int dia = inFile.nextInt();
                LocalTime hora = LocalTime.parse( inFile.next());

                Linea l = lineas.get(codLinea);
                if (l != null) {
                    l.agregarFrecuencia(dia, hora);
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("agregarFrecuencias: Archivo de frecuencias no encontrado: " + archivoFrecuencias, e);
        }
        catch(Exception e){
            LOGGER.error("agregarFrecuencias: Algo salio mal leyendo las frecuencias"+e.getMessage());
        } 
        finally {
            if (inFile != null)
                inFile.close();
        }
    }

    /**
     * Escribe las líneas y sus frecuencias a los archivos.
     */
    private void writeToFile(Map<String, Linea> lineas, String archivoLineas, String archivoFrecuencias) {
        Formatter outLineas = null;
        Formatter outFrecuencias = null;

        try {
            outLineas = new Formatter("src/main/resources/" + archivoLineas);
            outFrecuencias = new Formatter("src/main/resources/" + archivoFrecuencias);

            // Escribimos las líneas y paradas
            for (Linea l : lineas.values()) {
                outLineas.format("%s;%s", l.getCodigo(), l.getNombre());
                for (Parada p : l.getParadas()) {
                    outLineas.format(";%d", p.getCodigo());
                }
                outLineas.format("%n");

                // Escribimos sus frecuencias
                for(int i = 1; i<=7;i++){ // Del lunes al domingo
                    for (LocalTime hora : l.getFrecuencias(i)) {
                        outFrecuencias.format("%d;%s%n",i, hora);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            LOGGER.error("writeToFile: Error al crear archivo de líneas o frecuencias.", e);
        } catch (FormatterClosedException e) {
            LOGGER.error("writeToFile: Error al escribir en los archivos.", e);
        } finally {
            if (outLineas != null) outLineas.close();
            if (outFrecuencias != null) outFrecuencias.close();
        }
    }
}
