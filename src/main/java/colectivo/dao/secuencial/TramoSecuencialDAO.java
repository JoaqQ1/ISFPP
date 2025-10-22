package colectivo.dao.secuencial;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.TreeMap;

import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.util.Util;

public class TramoSecuencialDAO implements TramoDAO {

    private Map<String, Tramo> tramos;
    private ParadaDAO paradasDAO;
    private String name;
    private boolean actualizar;

    public TramoSecuencialDAO() {
        // Leemos el nombre del archivo desde secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle("secuencial");
        name = rb.getString("tramo");
        paradasDAO = new ParadaSecuencialDAO();
    }

    public Map<String, Tramo> buscarTodos() {
        if (tramos == null || actualizar) {
            tramos = readFromFile(name);
            actualizar = false;
        }
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
        Map<Integer,Parada> paradas = paradasDAO.buscarTodos();

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

        } catch (FileNotFoundException e) {
            System.err.println("Error: archivo no encontrado -> " + file);
        } catch (NoSuchElementException e) {
            System.err.println("Error en la estructura del archivo de tramos.");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Error leyendo el archivo de tramos.");
            e.printStackTrace();
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
            System.err.println("Error creando archivo de tramos.");
        } catch (FormatterClosedException e) {
            System.err.println("Error escribiendo archivo de tramos.");
        } finally {
            if (outFile != null)
                outFile.close();
        }
    }

}
