package colectivo.ui.impl.javafx.controllers;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.constantes.Constantes;
import colectivo.controlador.Coordinable;
import colectivo.controlador.CoordinadorApp; // Mantenemos la clase concreta solo para la firma de setCoordinador
import colectivo.controlador.ICoordinador;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.util.AsyncService;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controlador Principal (Orquestador).
 * <p>Responsabilidades reducidas:</p>
 * <ul>
 * <li>Recibir las inyecciones de FXML.</li>
 * <li>Inicializar y conectar los Micro-Controladores.</li>
 * <li>Manejar eventos globales (Menú, Logs, Salir).</li>
 * </ul>
 */
public class InterfazController implements Initializable, Coordinable {

    private static final Logger LOGGER = LogManager.getLogger(InterfazController.class);

    //<editor-fold defaultstate="collapsed" desc="1. FXML Variables (Se mantienen todas)">
    @FXML private AnchorPane anchorPaneFrmPrincipal;
    @FXML private VBox vboxResultados;
    @FXML private VBox contenedorRecorridos;
    @FXML private StackPane stackPaneResultados;

    // Labels
    @FXML private Label lblDestino;
    @FXML private Label lblDia;
    @FXML private Label lblHorario;
    @FXML private Label lblOrigen;
    @FXML private Label lblTitulo;
    @FXML private Label lblRutasDisponibles;

    // Inputs
    @FXML private ComboBox<String> cbxDia;
    @FXML private ComboBox<Parada> cbxOrigen;
    @FXML private ComboBox<Parada> cbxDestino;
    @FXML private Spinner<Integer> spinnerHora;
    @FXML private Spinner<Integer> spinnerMinuto;

    // Botones
    @FXML private Button btnMostrarRecorrido; 
    @FXML private Button btnCancelarCalculo;
    @FXML private Button btnLimpiarInterfaz;
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;

    // Componentes Especiales
    @FXML private SwingNode swingNodeMapa;
    @FXML private ProgressIndicator spinnerLoading;

    // Menú
    @FXML private ToggleGroup grupoIdiomas;
    @FXML private RadioMenuItem menuIdiomaEs;
    @FXML private RadioMenuItem menuIdiomaEn;
    @FXML private Menu menuConfig;
    @FXML private Menu menuConfigIdioma;
    @FXML private MenuItem menuConfigSalir;
    @FXML private MenuItem menuInfoVerLogs;
    @FXML private MenuItem menuMostrarParadas;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="2. Dependencias y Micro-Controladores">
    
    // El servicio async se crea aquí y se comparte
    private final AsyncService asyncService = new AsyncService();
    
    // Instanciamos los micro-controladores
    private final MapaController mapaController = new MapaController();
    private final BusquedaController busquedaController = new BusquedaController();
    
    // Referencia al Coordinador (usando la Interfaz)
    private ICoordinador coordinador;
    private ResourceBundle rb;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="3. Inicialización y Wiring">
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuración básica de UI que no requiere datos externos
        menuIdiomaEs.setToggleGroup(grupoIdiomas);
        menuIdiomaEn.setToggleGroup(grupoIdiomas);
        
        spinnerLoading.managedProperty().bind(spinnerLoading.visibleProperty());

        // Configuración básica de spinners (valores por defecto)
        spinnerHora.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));
        spinnerMinuto.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));

        LOGGER.info("InterfazController (Shell) inicializado.");
    }

    @Override
    public void setCoordinador(CoordinadorApp coordinadorImpl) {
        try {
        this.coordinador = coordinadorImpl;
        this.rb = coordinador.getResourceBundle();

        LOGGER.info("Iniciando Wiring (Conexión) de controladores...");

        // 1. Inicializar MAPA
        if (mapaController != null && swingNodeMapa != null) {
            mapaController.inicializarMapa(
                swingNodeMapa, 
                coordinador.getOrigenLatitud(), 
                coordinador.getOrigenLongitud(), 
                coordinador.getZoom()
            );
        } else {
            LOGGER.error("ERROR CRÍTICO: swingNodeMapa es NULL. Revisa el FXML fx:id='swingNodeMapa'");
        }

        // 2. Inicializar BUSQUEDA
        if (busquedaController != null) {
            busquedaController.configurarDependencias(coordinador, asyncService, mapaController);
            
            // Verifica que los componentes no sean null antes de pasarlos
            if (cbxOrigen == null || cbxDestino == null || cbxDia == null) {
                LOGGER.error("ERROR CRÍTICO: Algunos ComboBox son NULL. Revisa los fx:id en el FXML.");
                 // No retornamos para ver si explota más adelante y nos da más info
            }

            busquedaController.setControlesVisuales(
                cbxOrigen, cbxDestino, cbxDia,
                spinnerHora, spinnerMinuto,
                btnMostrarRecorrido, btnCancelarCalculo, btnLimpiarInterfaz,
                spinnerLoading, contenedorRecorridos
            );
            
            LOGGER.info("Cargando datos iniciales de UI...");
            busquedaController.inicializarDatosUI();
        }

        // 3. Actualizar textos locales
        actualizarTextosMenu();

        LOGGER.info("Wiring completado. Aplicación lista.");
        
    } catch (Exception e) {
        LOGGER.error("EXCEPCIÓN FATAL en setCoordinador:", e);
        e.printStackTrace(); // Imprime el error completo en consola
    }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="4. Delegación de Eventos (Búsqueda y Mapa)">
    
    @FXML
    private void handleCalcularRecorrido() {
        // DELEGACIÓN: "BusquedaController, haz lo tuyo"
        busquedaController.onCalcularRecorrido();
    }

    @FXML
    private void handleCancelarCalculo(ActionEvent event) {
        busquedaController.onLimpiarInterfaz(); 
    }

    @FXML
    private void handleLimpiarInterfaz() {
        busquedaController.onLimpiarInterfaz();
    }

    @FXML
    private void handleZoomIn() {
        mapaController.zoomIn();
    }

    @FXML
    private void handleZoomOut() {
        mapaController.zoomOut();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="5. Lógica Global (Menú, Idioma, Logs)">
    
    @FXML
    private void handleIdiomaChange(ActionEvent event) {
        if (menuIdiomaEs.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_ES);
        } else if (menuIdiomaEn.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_EN);
        }
        
        this.rb = coordinador.getResourceBundle();
        
        actualizarTextosMenu();
        
        busquedaController.actualizarTextosEIdioma();

        busquedaController.onLimpiarInterfaz();

        mapaController.limpiarMapa();
        
        LOGGER.info("Idioma global actualizado.");
    }

    private void actualizarTextosMenu() {
        if (rb == null) return;
        
        // Textos que están directamente en el FXML principal y no en sub-paneles
        lblTitulo.setText(rb.getString("app.title"));
        lblDia.setText(rb.getString("label.day") + ":");
        lblOrigen.setText(rb.getString("label.origin") + ":");
        lblDestino.setText(rb.getString("label.destination") + ":");
        lblHorario.setText(rb.getString("label.time") + ":");
        lblRutasDisponibles.setText(rb.getString("label.available_routes"));

        menuConfig.setText(rb.getString("menu.config"));
        menuConfigIdioma.setText(rb.getString("menu.config.language"));
        menuIdiomaEs.setText(rb.getString("menu.config.lang.es"));
        menuIdiomaEn.setText(rb.getString("menu.config.lang.en"));
        menuConfigSalir.setText(rb.getString("menu.config.exit"));
        menuMostrarParadas.setText(rb.getString("menu.debug.paint_stops"));
        menuInfoVerLogs.setText(rb.getString("menu.info.view_logs")); // Asumiendo que existe esta key
    }

    @FXML
    private void handleSalir(ActionEvent event) {
        // Limpieza correcta de recursos
        asyncService.shutdown();
        Platform.exit();
        System.exit(0);
    }

    /**
     * Refactorizado para usar AsyncService en lugar de Task anónimo.
     */
    @FXML
    private void handleVerLogs(ActionEvent event) {
        LOGGER.info("Abriendo visor de logs...");

        // 1. Configurar UI de la ventana de logs
        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced', monospace;");
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50); // Más pequeño
        
        StackPane layout = new StackPane(logTextArea, spinner);
        layout.setPadding(new Insets(10));
        
        Stage logStage = new Stage();
        logStage.setTitle(rb.getString("log.window_title"));
        logStage.initOwner(btnMostrarRecorrido.getScene().getWindow());
        logStage.setScene(new Scene(layout, 700, 500));
        logStage.show();

        // 2. Ejecutar carga con AsyncService
        spinner.setVisible(true);
        logTextArea.setText(rb.getString("log.loading"));

        asyncService.ejecutarAsync(
            () -> {
                // Background logic
                try {
                    String userHome = System.getProperty("user.home");
                    Path logPath = Paths.get(userHome, ".colectivo-logs", "mi_app.log");
                    if (!Files.exists(logPath)) return "No se encontró el archivo de log.";
                    return Files.readString(logPath);
                } catch (Exception e) {
                    throw new RuntimeException("Error leyendo logs", e);
                }
            },
            contenidoLog -> {
                // UI Thread (Success)
                spinner.setVisible(false);
                logTextArea.setText(contenidoLog);
                logTextArea.positionCaret(logTextArea.getLength());
            },
            error -> {
                // UI Thread (Error)
                spinner.setVisible(false);
                logTextArea.setText(rb.getString("log.load_error") + ": " + error.getMessage());
            }
        );
    }

    @FXML
    private void handleDebugPintarParadas(ActionEvent event) {
    LOGGER.info("DEBUG: Solicitando datos de paradas y líneas...");
        
        // Feedback visual rápido
        spinnerLoading.setVisible(true);

        asyncService.ejecutarAsync(
            // 1. Background (InterfazController): Obtener Datos Crudos
            () -> {
                // Recolectamos los datos necesarios del coordinador
                Collection<Parada> paradas = coordinador.listarParadas().values();
                Collection<Linea> lineas = coordinador.listarLineas().values();
                return new DatosDebug(paradas, lineas);
            },
            
            // 2. Success (UI Thread): Pasar datos al MapaController
            datos -> {
                spinnerLoading.setVisible(false);
                LOGGER.info("Datos obtenidos. Delegando dibujado al MapaController.");
                // Aquí llamamos al método que creamos arriba
                mapaController.dibujarModoDebug(datos.paradas, datos.lineas);
            },
            
            // 3. Error
            error -> {
                spinnerLoading.setVisible(false);
                LOGGER.error("Error obteniendo datos debug", error);
            }
        );
    }

    // Clase helper simple para transportar los dos tipos de listas
    private static class DatosDebug {
        final Collection<Parada> paradas;
        final Collection<Linea> lineas;
        public DatosDebug(Collection<Parada> p, Collection<Linea> l) {
            this.paradas = p;
            this.lineas = l;
        }
    }

    //</editor-fold>
}