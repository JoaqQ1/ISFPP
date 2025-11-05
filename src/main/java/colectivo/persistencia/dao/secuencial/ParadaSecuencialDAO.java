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

import colectivo.constantes.Constantes;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.util.Util;

public class ParadaSecuencialDAO implements ParadaDAO {

    private static final Logger LOGGER = LogManager.getLogger(ParadaSecuencialDAO.class.getName());
    
    private Map<Integer, Parada> paradas;
    private String name;
    private boolean actualizar;


    public ParadaSecuencialDAO() {
        // Leemos el nombre del archivo desde el secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle(Constantes.PATH_DATA_TXT);
        name = rb.getString("parada");
        LOGGER.info("ParadaSecuencialDAO inicializado con archivo: " + name);
    }

    public Map<Integer, Parada> buscarTodos() {
        if (paradas == null || actualizar) {
            paradas = readFromFile(name);
            actualizar = false;
        }
        LOGGER.info("Paradas cargadas desde archivo: " + name);
        return paradas;
    }

    public void insertar(Parada parada) {
        paradas.put(parada.getCodigo(), parada);
        writeToFile(paradas, name);
        actualizar = true;
    }

    public void actualizar(Parada parada) {
        paradas.put(parada.getCodigo(), parada);
        writeToFile(paradas, name);
        actualizar = true;
    }

    public void borrar(Parada parada) {
        paradas.remove(parada.getCodigo());
        writeToFile(paradas, name);
        actualizar = true;
    }

    // ---------------------------------------------------
    // Métodos auxiliares para leer y escribir en archivo
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
        } catch (FileNotFoundException e) {

            LOGGER.error("readFromFile: Error opening file: " + file, e);
        } catch (NoSuchElementException e) {
            LOGGER.error("readFromFile: Error in file record structure", e);
        } catch (IllegalStateException e) {
            LOGGER.error("readFromFile: Error reading from file", e);
        } finally {
            if (inFile != null)
                inFile.close();
        }

        return map;
    }

    private void writeToFile(Map<Integer, Parada> paradas, String file) {
        Formatter outFile = null;
        try {
            outFile = new Formatter("src/main/resources/" + file);
            for (Parada p : paradas.values()) {
                outFile.format("%d;%s;%.6f;%.6f;%n",
                        p.getCodigo(),
                        p.getDireccion(),
                        p.getLatitud(),
                        p.getLongitud());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("writeToFile: Error creating file.", e);
        } catch (FormatterClosedException e) {
            LOGGER.error("writeToFile: Error writing to file.", e);
        } finally {
            if (outFile != null)
                outFile.close();
        }
    }
}
