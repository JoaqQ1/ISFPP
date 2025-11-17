package colectivo.persistencia.dao.secuencial;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.TreeMap;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.excepciones.ConfiguracionException;
import colectivo.excepciones.FactoryException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.persistencia.dao.TramoDAO;
import colectivo.util.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TramoSecuencialDAO implements TramoDAO {

    private static final Logger LOGGER = LogManager.getLogger(TramoSecuencialDAO.class.getName());
    private Map<String, Tramo> tramos;
    private String name;
    private boolean actualizar;

    public TramoSecuencialDAO() {
        // 1. Obtener la configuración global
        ConfiguracionGlobal config = ConfiguracionGlobal.getConfiguracionGlobal();

        // 2. Obtener el código de la ciudad actual (ej: "CO")
        String ciudadActual = config.getCiudadActual();

        // 3. Leemos el nombre del archivo desde secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle(Constantes.PATH_DATA_TXT);
        
        // 4. Construir la clave dinámica
        String claveTramo = "tramo." + ciudadActual; // Ej: "tramo.CO"

        // 5. Obtener el nombre del archivo
        name = rb.getString(claveTramo);

        LOGGER.info("TramoSecuencialDAO inicializado para la ciudad: " + ciudadActual + " con archivo: " + name);
    }

    public Map<String, Tramo> buscarTodos() {
        if (tramos == null || actualizar) {
            tramos = readFromFile(name);
            actualizar = false;
        }
        LOGGER.info("Tramos cargados desde archivo: " + name);
        return tramos;
    }


    // ---------------------------------------------------
    // Métodos auxiliares para leer 
    // ---------------------------------------------------

    /**
     * Lee todos los tramos desde el archivo especificado.
     * 
     * Formato esperado de cada línea:
     * codParadaInicio;codParadaFin;tiempo;tipo
     */
    private Map<String, Tramo> readFromFile(String file) {
        Map<String, Tramo> map = new TreeMap<>();
        Scanner inFile = null;
        try {

            Map<Integer,Parada> paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();

            inFile = new Scanner(new File("src/main/resources/" + file));
            inFile.useDelimiter("\\s*;\\s*");

            while (inFile.hasNext()) {
                int codInicio = inFile.nextInt();
                int codFin = inFile.nextInt();
                int tiempo = inFile.nextInt();
                int tipo = inFile.nextInt();
                Parada inicio = paradas.get(codInicio);
                Parada fin = paradas.get(codFin);
                
                Tramo tramo = new Tramo(inicio, fin, tiempo, tipo);
                
                map.put(Util.claveTramo(inicio, fin), tramo);
            }
            LOGGER.info("Tramos cargados desde archivo: " + file);
            return map;
        } catch (FileNotFoundException e) {
            String errorMsg = "No se encontró el archivo de tramos: " + file;
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } catch (NoSuchElementException e) {
            String errorMsg = String.format(
                "Error de formato en el archivo de tramos '%s'. Se esperaba una línea con formato 'int;int;int;int' (codInicio;codFin;tiempo;tipo).", file
            );
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } catch (FactoryException e) {
            // Error si el DAO de Paradas falla al cargarse
            String errorMsg = "Error de dependencia: No se pudo obtener ParadaDAO desde la Factory para leer los tramos.";
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Error inesperado al procesar el archivo de tramos '" + file + "'.";
            LOGGER.error(errorMsg, e);
            throw new ConfiguracionException(errorMsg, e); 
        } finally {
            if (inFile != null)
                inFile.close();
        }
    }


}
