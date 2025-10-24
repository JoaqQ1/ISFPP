package colectivo.dao.secuencial;

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

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.dao.LineaDAO;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;

public class LineaSecuencialDAO implements LineaDAO {

    private Map<String, Linea> lineas;
    private String archivoLineas;
    private String archivoFrecuencias;
    private boolean actualizar;
    
    public LineaSecuencialDAO() {
        // Leemos los nombres de archivos desde secuencial.properties
        ResourceBundle rb = ResourceBundle.getBundle("secuencial");
        archivoLineas = rb.getString("linea");
        archivoFrecuencias = rb.getString("frecuencia");
    }

    @Override
    public Map<String, Linea> buscarTodos() {
        if (lineas == null || actualizar) {
            lineas = readFromFile(archivoLineas, archivoFrecuencias);
            actualizar = false;
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
        Map<Integer,Parada> paradas = ((ParadaDAO) Factory.getInstancia(Constantes.PARADA)).buscarTodos();
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

        } catch (FileNotFoundException e) {
            System.err.println("Archivo no encontrado: " + archivoLineas);
        } catch (NoSuchElementException e) {
            System.err.println("Error en la estructura del archivo de líneas.");
        } finally {
            if (inFile != null)
                inFile.close();
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
                LocalTime hora = LocalTime.parse(inFile.next());

                Linea l = lineas.get(codLinea);
                if (l != null) {
                    l.agregarFrecuencia(dia, hora);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("Archivo de frecuencias no encontrado: " + archivoFrecuencias);
        } finally {
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
            System.err.println("Error al crear archivo de líneas o frecuencias.");
        } catch (FormatterClosedException e) {
            System.err.println("Error al escribir en los archivos.");
        } finally {
            if (outLineas != null) outLineas.close();
            if (outFrecuencias != null) outFrecuencias.close();
        }
    }
}
