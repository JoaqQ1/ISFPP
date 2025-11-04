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

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
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
        // Leemos el nombre del archivo desde secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle(Constantes.PATH_DATA_TXT);
        name = rb.getString("tramo");
        LOGGER.info("TramoSecuencialDAO inicializado con archivo: " + name);
    }

    public Map<String, Tramo> buscarTodos() {
        if (tramos == null || actualizar) {
            tramos = readFromFile(name);
            actualizar = false;
        }
        LOGGER.info("Tramos cargados desde archivo: " + name);
        return tramos;
    }

    public void insertar(Tramo tramo) {
        tramos.put(Util.claveTramo(tramo.getInicio(), tramo.getFin()), tramo);
        writeToFile(tramos, name);
        actualizar = true;
    }

    public void actualizar(Tramo tramo) {
        tramos.put(Util.claveTramo(tramo.getInicio(), tramo.getFin()), tramo);
        writeToFile(tramos, name);
        actualizar = true;
    }

    public void borrar(Tramo tramo) {
        tramos.remove(Util.claveTramo(tramo.getInicio(), tramo.getFin()));
        writeToFile(tramos, name);
        actualizar = true;
    }

    // ---------------------------------------------------
    // Métodos auxiliares para leer y escribir en archivo
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
        Map<Integer,Parada> paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();

        try {
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
        } catch (FileNotFoundException e) {
            LOGGER.error("Error: archivo no encontrado -> " + file, e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error en la estructura del archivo de tramos.", e);
        } catch (IllegalStateException e) {
            LOGGER.error("Error leyendo el archivo de tramos.", e);
        } finally {
            if (inFile != null)
                inFile.close();
        }

        return map;
    }

    /**
     * Escribe todos los tramos al archivo especificado.
     * 
     * Formato guardado:
     * codParadaInicio;codParadaFin;tiempo;tipo
     */
    private void writeToFile(Map<String, Tramo> tramos, String file) {
        Formatter outFile = null;
        try {
            outFile = new Formatter("src/main/resources/" + file);
            for (Tramo t : tramos.values()) {
                outFile.format("%d;%d;%d;%d;%n",
                        t.getInicio().getCodigo(),
                        t.getFin().getCodigo(),
                        t.getTiempo(),
                        t.getTipo());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Error creando archivo de tramos.", e);
        } catch (FormatterClosedException e) {
            LOGGER.error("Error escribiendo archivo de tramos.", e);
        } finally {
            if (outFile != null)
                outFile.close();
        }
    }

}
