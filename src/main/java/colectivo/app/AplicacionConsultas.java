package colectivo.app;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.controlador.CoordinadorApp;


/**
 * Clase principal de la aplicación de consultas del sistema de colectivos.
 * Permite al usuario calcular y mostrar recorridos entre paradas según día y hora.
 */
public class AplicacionConsultas {

    /** Coordinador que actúa como intermediario entre la interfaz y el sistema. */
    private static CoordinadorApp coordinador;

    private static final Logger LOGGER = LogManager.getLogger(AplicacionConsultas.class.getName());


    /**
     * Método principal que inicia la aplicación.
     * @param args argumentos de línea de comando (no utilizados)
     * @throws IOException si ocurre un error al cargar los datos del sistema
     */
    public static void main(String[] args) throws IOException {
        try {
            // La creación manual con 'new' está bien aquí. El coordinador se encarga de la DI interna.
            coordinador = new CoordinadorApp();
            coordinador.inicializarAplicacion();
            LOGGER.info("Aplicación de consultas iniciada correctamente.");

        } catch (Exception e) {
            LOGGER.error("Error al iniciar la aplicación de consultas: " + e.getMessage());
            System.exit(1);
        }
    }


}
