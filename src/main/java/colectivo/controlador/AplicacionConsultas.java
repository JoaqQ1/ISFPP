package colectivo.controlador;

import java.io.IOException;

import colectivo.configuracion.ConfiguracionGlobal;

/**
 * Clase principal de la aplicación de consultas del sistema de colectivos.
 * Permite al usuario calcular y mostrar recorridos entre paradas según día y hora.
 */
public class AplicacionConsultas {

    /** Coordinador que actúa como intermediario entre la interfaz y el sistema. */
    private static CoordinadorApp coordinador;



    /**
     * Método principal que inicia la aplicación.
     * @param args argumentos de línea de comando (no utilizados)
     * @throws IOException si ocurre un error al cargar los datos del sistema
     */
    public static void main(String[] args) throws IOException {
        ConfiguracionGlobal config = ConfiguracionGlobal.geConfiguracionGlobal();
        System.out.println("Iniciando " + config.get("persistencia.tipo") );
        try {
            // La creación manual con 'new' está bien aquí. El coordinador se encarga de la DI interna.
            coordinador = new CoordinadorApp();
            coordinador.inicializarAplicacion();

        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            System.exit(1);
        }
    }


}
