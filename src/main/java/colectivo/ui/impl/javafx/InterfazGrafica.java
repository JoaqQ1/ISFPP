package colectivo.ui.impl.javafx;

import colectivo.controlador.CoordinadorApp;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clase Launcher (Puente Estático).
 * Extiende de Application y maneja el ciclo de vida de JavaFX.
 * Su única responsabilidad es lanzar la UI y pasar las dependencias.
 */
public class InterfazGrafica extends Application {

    private static final Logger LOGGER = LogManager.getLogger(InterfazGrafica.class.getName());

    // --- INICIO DEL PUENTE ESTÁTICO ---
    // Guardan las instancias originales pasadas desde 'iniciar()'
    private static CoordinadorApp coordinadorEstatico;
    private static InterfazJavaFXImpl uiInstanciaEstatica;
    // --- FIN DEL PUENTE ESTÁTICO ---

    /**
     * Constructor vacío llamado por JavaFX internamente.
     */
    public InterfazGrafica() {
        LOGGER.debug("Constructor de InterfazGrafica (Launcher) llamado por JavaFX.");
    }

    /**
     * Método de entrada llamado por InterfazJavaFXImpl.
     * Establece el puente estático antes de lanzar JavaFX.
     */
    public static void launchApp(CoordinadorApp coordinador, InterfazJavaFXImpl uiImpl, String[] args) {
        LOGGER.info("Estableciendo puente estático (Hashing UI: {})...", uiImpl.hashCode());
        coordinadorEstatico = coordinador;
        uiInstanciaEstatica = uiImpl;
        
        // Llama al 'start()' de esta misma clase, pero en el hilo de JavaFX
        launch(args); 
    }

    /**
     * Método principal de JavaFX, se ejecuta después de launch().
     * Aquí orquestamos la inicialización usando el puente.
     */
    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("JavaFX 'start' alcanzado. Cruzando el puente...");
        
        try {
            // 1. Recuperar la instancia ORIGINAL de UI desde el puente
            InterfazJavaFXImpl uiOriginal = uiInstanciaEstatica;
            
            if (uiOriginal == null) {
                LOGGER.fatal("Error fatal: El puente estático es nulo. No se puede iniciar la UI.");
                throw new IllegalStateException("uiInstanciaEstatica no fue inicializada.");
            }

            // 2. ORQUESTAR la secuencia de 3 pasos (Recomendación de Cátedra)
            uiOriginal.setPrimaryStage(primaryStage); // Paso 1: Setear Stage
            uiOriginal.inicializarUI();           // Paso 2: Cargar FXML y lógica
            uiOriginal.mostrarVistaPrincipal();   // Paso 3: Mostrar

        } catch (Exception e) {
            LOGGER.error("Error catastrófico en el método start() de InterfazGrafica", e);
            // Si todo falla, cerramos
            Platform.exit();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        // Buena práctica: limpiar el puente al cerrar
        LOGGER.info("Limpiando puente estático al cerrar la aplicación.");
        coordinadorEstatico = null;
        uiInstanciaEstatica = null;
    }
}