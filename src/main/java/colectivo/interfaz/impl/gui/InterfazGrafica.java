package colectivo.interfaz.impl.gui;

import colectivo.controlador.Coordinador;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InterfazGrafica extends Application {

    // Necesitamos una referencia estática para pasar el coordinador
    private static Coordinador coordinadorEstatico;

    // Método que el InterfazService usará para iniciar la GUI
    public static void launchApp(Coordinador coordinador, String[] args) {
        coordinadorEstatico = coordinador;
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        // El FXML ahora asume que el controlador es InterfazController
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("views/mainscene.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/views/styles/estilos.css").toExternalForm());
        // Conectar el Controlador con el Coordinador
        InterfazController controller = loader.getController();
        controller.setCoordinador(coordinadorEstatico); // Se inyecta la dependencia
        controller.cargarInterfaz();              // Se cargan los ComboBox

        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulación de Colectivos");
        primaryStage.show();
    }
}