package colectivo.dao.secuencial;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.ResourceBundle;

import colectivo.dao.ParadaDAO;
import colectivo.modelo.Parada;
import colectivo.util.Util;

public class ParadaSecuencialDAO implements ParadaDAO {

    private static Map<Integer, Parada> paradas;
    private String name;
    private boolean actualizar;

    public ParadaSecuencialDAO() {
        // Leemos el nombre del archivo desde el secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle("secuencial");
        name = rb.getString("parada");
    }

    public Map<Integer, Parada> buscarTodos() {
        if (paradas == null || actualizar) {
            paradas = readFromFile(name);
            actualizar = false;
        }
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

        } catch (FileNotFoundException e) {
            System.err.println("Error opening file: " + file);
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            System.err.println("Error in file record structure");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Error reading from file.");
            e.printStackTrace();
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
            System.err.println("Error creating file.");
        } catch (FormatterClosedException e) {
            System.err.println("Error writing to file.");
        } finally {
            if (outFile != null)
                outFile.close();
        }
    }
}
