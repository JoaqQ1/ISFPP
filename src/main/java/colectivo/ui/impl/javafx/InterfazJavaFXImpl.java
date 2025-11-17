package colectivo.ui.impl.javafx;

import colectivo.controlador.CoordinadorApp;
import colectivo.ui.Interfaz;
import colectivo.ui.impl.javafx.controllers.InterfazController;

import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import org.apache.logging.log4j.*;

/**
 * Implementación Concreta de la UI (Vista).
 * NO extiende de Application. Sigue las directrices de la cátedra.
 */
public class InterfazJavaFXImpl implements Interfaz {

    private static final Logger LOGGER = LogManager.getLogger(InterfazJavaFXImpl.class.getName());
    
    // --- Dependencias Inyectadas ---
    private CoordinadorApp coordinador;
    private Stage primaryStage; // El Stage principal, inyectado por el Launcher
    private InterfazController controller; // El controlador FXML

    @Override
    public void setCoordinador(CoordinadorApp coordinador) {
        this.coordinador = coordinador;
    }

    /**
     * Punto de entrada llamado por el CoordinadorApp.
     * Delega el lanzamiento al Launcher estático.
     */
    @Override
    public void iniciar() {
        LOGGER.info("Iniciando UI (Instancia: {})...", this.hashCode());
        // Llama al Launcher estático pasando esta misma instancia (el puente)
        InterfazGrafica.launchApp(coordinador, this, new String[]{});
    }

    // --- CICLO DE VIDA (Orquestado por InterfazGrafica.start()) ---

    /**
     * Paso 1: Recibe el Stage desde el Launcher (Inyección).
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Paso 2: Prepara la UI (Carga FXML, CSS, Controladores).
     * Esta es la lógica principal de tu antiguo método 'inicializarLogicaUI'.
     */
    public void inicializarUI() {
        // 3. Manejo básico de errores
        try {
            if (coordinador == null) {
                LOGGER.fatal("Error: El coordinador es nulo. La UI no puede inicializarse.");
                throw new IllegalStateException("Coordinador no inyectado.");
            }
            
            LOGGER.info("Cargando ResourceBundle y FXML...");
            ResourceBundle rb = this.coordinador.getResourceBundle();
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/mainscene.fxml"), rb);
            
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            scene.getStylesheets().add(getClass().getResource("/views/styles/estilos.css").toExternalForm());
            
            // Guardamos el controlador para inyectarle dependencias
            this.controller = loader.getController();
            
            // Inyectamos el coordinador en el InterfazController (FXML)
            this.controller.setCoordinador(this.coordinador);
            
            primaryStage.setScene(scene);
            primaryStage.setTitle(rb.getString("app.title"));

        } catch (Exception e) {
            LOGGER.error("inicializarUI: Error fatal inicializando la interfaz.", e);
            // Si falla la carga del FXML, la app no puede continuar.
            // (Aquí podrías mostrar una alerta nativa si quisieras)
            Platform.exit();
            System.exit(1);
        }
    }

    /**
     * Paso 3: Muestra la vista al usuario.
     */
    public void mostrarVistaPrincipal() {
        if (primaryStage != null) {
            primaryStage.show();
            LOGGER.info("Vista principal mostrada.");
        } else {
            LOGGER.error("mostrarVistaPrincipal: Stage es nulo. No se puede mostrar la vista.");
        }
    }
}