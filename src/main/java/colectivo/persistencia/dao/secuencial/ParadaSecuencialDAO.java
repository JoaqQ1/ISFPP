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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.constantes.Constantes;
import colectivo.excepciones.ConfiguracionException;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.util.Util;

public class ParadaSecuencialDAO implements ParadaDAO {

    private static final Logger LOGGER = LogManager.getLogger(ParadaSecuencialDAO.class.getName());
    
    private Map<Integer, Parada> paradas;
    private String name;
    private boolean actualizar;


    public ParadaSecuencialDAO() {
        // 1. Obtener la configuración global
        ConfiguracionGlobal config = ConfiguracionGlobal.getConfiguracionGlobal();

        // 2. Obtener el código de la ciudad actual (ej: "CO")
        String ciudadActual = config.getCiudadActual();
        
        // 3. Leemos el nombre del archivo desde el secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle(Constantes.PATH_DATA_TXT);

        // 4. Construir la clave dinámica
        String claveParada = "parada." + ciudadActual; // Ej: "parada.CO"
        
        // 5. Obtener el nombre del archivo
        name = rb.getString(claveParada);
        
        LOGGER.info("ParadaSecuencialDAO inicializado para la ciudad: " + ciudadActual + " con archivo: " + name);
    }

    public Map<Integer, Parada> buscarTodos() {
        if (paradas == null || actualizar) {
            paradas = readFromFile(name);
            actualizar = false;
        }
        LOGGER.info("Paradas cargadas desde archivo: " + name);
        return paradas;
    }


    // ---------------------------------------------------
    // Métodos auxiliares para leer
    // ---------------------------------------------------

    private Map<Integer, Parada> readFromFile(String file) {
        Map<Integer, Parada> map = new TreeMap<>();
        Scanner inFile = null;
        try {
            
            inFile = new Scanner(new File("src/main/resources/" + file));
            inFile.useDelimiter("\\s*;\\s*");
            
            while (inFile.hasNext()) {
                int codParada = inFile.nextInt();
                String direccion = inFile.next();
                double latitud = Util.parsearDecimalConComa(inFile.next());
                double longitud = Util.parsearDecimalConComa(inFile.next());

                map.put(codParada, new Parada(codParada, direccion, latitud, longitud));
            }
            LOGGER.info("Paradas cargadas desde archivo: " + file);
            return map;
        } catch (FileNotFoundException e) {

            LOGGER.error("readFromFile: Error opening file: " + file, e);
            throw new ConfiguracionException("Error archivo de paradas no encontrado: "+file,e);
        } catch (NoSuchElementException e) {
            LOGGER.error("readFromFile: Error in file record structure", e);
            throw new ConfiguracionException("Error en la estructura del archivo",e);
        } catch (IllegalStateException e) {
            LOGGER.error("readFromFile: Error reading from file", e);
            throw new ConfiguracionException("Error al leer el archivo de paradas",e);
        } finally {
            if (inFile != null)
                inFile.close();
        }

    }

}
