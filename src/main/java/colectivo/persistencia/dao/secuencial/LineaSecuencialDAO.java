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
import colectivo.excepciones.ConfiguracionException;
import colectivo.excepciones.FactoryException;
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


    // ---------------------------------------------------
    // Métodos auxiliares para leer archivos
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
        try {

            Map<Integer,Parada> paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();
            
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
            return map;
        } catch (FileNotFoundException e) {
            String errorMsg = "No se encontró el archivo de líneas: " + archivoLineas;
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } catch (NoSuchElementException e) {
            String errorMsg = "Error de formato en línea:" +   e.getMessage();
            LOGGER.error(errorMsg,e);
            throw new ConfiguracionException(errorMsg, e);

        } catch(FactoryException e){
            String errorMsg = "Error de dependencia: No se pudo obtener ParadaDAO desde la Factory para leer las líneas.";
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
            
        } catch(Exception e){
            String errorMsg = "Error inesperado al procesar el archivo de líneas '" + archivoLineas + "'.";
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } 
        finally {
            if (inFile != null)
                inFile.close();
            LOGGER.info("Lectura de líneas finalizada.");
        }
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
            String errorMsg = "No se encontró el archivo de frecuencias: " + archivoFrecuencias;
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } catch(NoSuchElementException e){
            String errorMsg = String.format(
                "Error de formato en el archivo de frecuencias '%s'. Se esperaba una línea con formato 'string;int;HH:MM' (codLinea;dia;hora).", archivoFrecuencias
            );
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        }
        catch (java.time.format.DateTimeParseException e) {
            String errorMsg = String.format(
                "Error de formato de hora en el archivo de frecuencias '%s'. La hora no está en formato HH:MM (ej: 08:30). Error: %s", archivoFrecuencias, e.getMessage()
            );
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = "Error inesperado al procesar el archivo de frecuencias '" + archivoFrecuencias + "'.";
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        }
        finally {
            if (inFile != null)
                inFile.close();
        }
    }

    
}
