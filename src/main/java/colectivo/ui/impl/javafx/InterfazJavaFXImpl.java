package colectivo.ui.impl.javafx;

import colectivo.controlador.CoordinadorApp;
import colectivo.ui.Interfaz;
import colectivo.ui.impl.javafx.controllers.InterfazController;

import java.util.ResourceBundle; // Importaciones necesarias
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.*;
public class InterfazJavaFXImpl implements Interfaz {

    private static final Logger LOGGER = LogManager.getLogger(InterfazJavaFXImpl.class.getName());
    private CoordinadorApp coordinador;

    @Override
    public void setCoordinador(CoordinadorApp coordinador) {
        this.coordinador = coordinador;
    }

    @Override
    public void iniciar() {
        // Ahora pasamos 'this' (esta misma instancia) al launcher
        InterfazGrafica.launchApp(coordinador, this, new String[]{});
        LOGGER.info("Interfaz JavaFX iniciada.");
    }

    /**
     * NUEVO MÉTODO: Esta es la lógica que antes estaba en InterfazGrafica.start().
     * El launcher llamará a este método desde su 'start()'.
     */
    public void inicializarLogicaUI(Stage primaryStage) {
        try {
            // 1. Cargar el ResourceBundle (i18n)
            // Usamos 'this.coordinador', que fue inyectado por el CoordinadorApp
            ResourceBundle rb = this.coordinador.getResourceBundle();
            
            // 2. Crear el FXMLLoader
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/mainscene.fxml"), rb);
            
            // 3. Cargar la escena
            Scene scene = new Scene(loader.load());
            
            // 4. Cargar la hoja de estilos CSS
            scene.getStylesheets().add(getClass().getResource("/views/styles/estilos.css").toExternalForm());
            
            // 5. Obtener el controlador creado por el FXMLLoader
            
            InterfazController controller = loader.getController();
            
            // 6. Inyectar dependencias en el controlador
            // Usamos 'this.coordinador' (la instancia original)

            controller.setCoordinador(this.coordinador);
            
            // 7. Cargar datos iniciales
            // controller.cargarInterfaz();

            // 8. Configurar y mostrar la ventana principal
            primaryStage.setScene(scene);
            primaryStage.setTitle(rb.getString("app.title"));
            primaryStage.show();
            LOGGER.info("Interfaz JavaFX inicializada y mostrada.");

        } catch (Exception e) {
            LOGGER.error("inicializarLogicaUI: Error inicializando la interfaz JavaFX.", e);
            // Manejar la excepción adecuadamente
        }
    }
}