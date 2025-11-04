package colectivo.ui.impl.javafx;


import colectivo.controlador.CoordinadorApp;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Clase principal de la Interfaz Gráfica (GUI).
 * Se encarga de lanzar la aplicación JavaFX y cargar la escena inicial.
 * Esta versión carga el idioma (ResourceBundle) UNA SOLA VEZ al inicio.
 */
public class InterfazGrafica extends Application {

    // Referencia estática al coordinador para pasarlo entre el lanzador y la app
    private static CoordinadorApp coordinadorEstatico;

    // Esta es la pieza clave que faltaba:
    private static InterfazJavaFXImpl uiInstanciaEstatica;

    public InterfazGrafica() {
    }
    /**
     * Método de entrada principal llamado por el InterfazService.
     * Recibe el coordinador y lo guarda estáticamente antes de lanzar JavaFX.
     * * @param coordinador La instancia del coordinador principal.
     * @param args Argumentos de línea de comandos.
     */
    public static void launchApp(CoordinadorApp coordinador, InterfazJavaFXImpl uiImpl, String[] args) {
        coordinadorEstatico = coordinador;
        uiInstanciaEstatica = uiImpl;
        launch(args); // Llama al 'start()' de esta misma clase
    }

    /**
     * Método principal de JavaFX, se ejecuta después de launch().
     * Aquí es seguro acceder a 'coordinadorEstatico'.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // 1. Recuperar la instancia ORIGINAL de UI desde el puente
        InterfazJavaFXImpl uiOriginal = uiInstanciaEstatica;
        
        // 2. DELEGAR toda la lógica de FXML/UI a esa instancia
        uiOriginal.inicializarLogicaUI(primaryStage);
    }
    @Override
    public void stop() {
        // Buena práctica: limpiar el puente al cerrar
        coordinadorEstatico = null;
        uiInstanciaEstatica = null;
    }
}