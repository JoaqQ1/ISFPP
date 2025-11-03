package colectivo.interfaz.impl.gui;

import java.util.ResourceBundle;

import colectivo.controlador.CoordinadorApp;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal de la Interfaz Gráfica (GUI).
 * Se encarga de lanzar la aplicación JavaFX y cargar la escena inicial.
 * Esta versión carga el idioma (ResourceBundle) UNA SOLA VEZ al inicio.
 */
public class InterfazGrafica extends Application {

    // Referencia estática al coordinador para pasarlo entre el lanzador y la app
    private static CoordinadorApp coordinadorEstatico;

    // Almacena la configuración de idioma (cargada en start())
    private ResourceBundle rb;


    /**
     * Método de entrada principal llamado por el InterfazService.
     * Recibe el coordinador y lo guarda estáticamente antes de lanzar JavaFX.
     * * @param coordinador La instancia del coordinador principal.
     * @param args Argumentos de línea de comandos.
     */
    public static void launchApp(CoordinadorApp coordinador, String[] args) {
        coordinadorEstatico = coordinador;
        launch(args); // Llama al 'start()' de esta misma clase
    }

    /**
     * Método principal de JavaFX, se ejecuta después de launch().
     * Aquí es seguro acceder a 'coordinadorEstatico'.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        // 1. Cargar el ResourceBundle (i18n) desde el coordinador.
        // Esto es seguro porque 'start()' se ejecuta después de 'launchApp()'.
        rb = coordinadorEstatico.getResourceBundle();
        
        // 2. Crear el FXMLLoader, pasándole la ruta del FXML y el ResourceBundle.
        // Al pasar 'rb' aquí, FXML puede resolver todas las claves (%) automáticamente.
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/mainscene.fxml"), rb);
    
        // 3. Cargar la escena
        Scene scene = new Scene(loader.load());
        
        // 4. Cargar la hoja de estilos CSS
        scene.getStylesheets().add(getClass().getResource("/views/styles/estilos.css").toExternalForm());
        
        // 5. Obtener el controlador creado por el FXMLLoader
        InterfazController controller = loader.getController();
        
        // 6. Inyectar dependencias en el controlador
        controller.setCoordinador(coordinadorEstatico);
        
        // 7. Cargar datos iniciales en la interfaz (ComboBoxes, etc.)
        controller.cargarInterfaz();

        // 8. Configurar y mostrar la ventana principal (Stage)
        primaryStage.setScene(scene);
        primaryStage.setTitle(rb.getString("app.title")); // Título internacionalizado
        primaryStage.show();
    }
}