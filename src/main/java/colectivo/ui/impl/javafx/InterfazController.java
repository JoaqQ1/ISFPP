package colectivo.ui.impl.javafx;

import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import colectivo.constantes.Constantes;
import colectivo.controlador.Coordinable;
import colectivo.controlador.CoordinadorApp;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javax.swing.SwingUtilities;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador principal de la interfaz gráfica de usuario (GUI) de la aplicación de colectivos.
 * * <p>Esta clase conecta los elementos visuales del FXML con la lógica del programa, permitiendo
 * al usuario seleccionar una parada de origen, destino, día y hora para calcular posibles recorridos.</p>
 * * <p>También administra la integración del mapa (usando Swing dentro de JavaFX) y se comunica
 * con el objeto {@link CoordinadorApp}, que maneja la lógica del dominio.</p>
 */
public class InterfazController implements Initializable, Coordinable {

    //<editor-fold defaultstate="collapsed" desc="1. FXML Variables">
    // --- 1.1. Contenedores y Paneles Principales ---
    @FXML private AnchorPane anchorPaneFrmPrincipal;
    @FXML private VBox vboxResultados;
    @FXML private VBox contenedorRecorridos;
    @FXML private StackPane stackPaneResultados;

    // --- 1.2. Etiquetas (Labels) ---
    @FXML private Label lblDestino;
    @FXML private Label lblDia;
    @FXML private Label lblHorario;
    @FXML private Label lblOrigen;
    @FXML private Label lblTitulo;
    @FXML private Label lblRutasDisponibles;

    // --- 1.3. Controles de Formulario (ComboBox) ---
    @FXML private ComboBox<String> cbxDia;
    @FXML private ComboBox<Parada> cbxOrigen;
    @FXML private ComboBox<Parada> cbxDestino;
    // @FXML private ComboBox<String> cbxHora;
    // @FXML private ComboBox<String> cbxMinuto;
    @FXML private Spinner<Integer> spinnerHora;
    @FXML private Spinner<Integer> spinnerMinuto;

    // --- 1.4. Botones (Buttons) ---
    @FXML private Button btnMostrarRecorrido; 
    @FXML private Button btnCancelarCalculo;
    @FXML private Button btnLimpiarInterfaz;
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;

    // --- 1.5. Indicadores y Nodos Especiales ---
    @FXML private SwingNode swingNodeMapa;
    @FXML private ProgressIndicator spinnerLoading;

    // --- 1.6. Elementos del Menú ---
    @FXML private ToggleGroup grupoIdiomas;
    @FXML private RadioMenuItem menuIdiomaEs;
    @FXML private RadioMenuItem menuIdiomaEn;
    @FXML private Menu menuConfig;
    @FXML private Menu menuConfigIdioma;
    @FXML private MenuItem menuConfigSalir;
    @FXML private MenuItem menuInfoVerLogs;
    @FXML private MenuItem menuMostrarParadas;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="2. Private Fields">
    private JXMapViewer mapViewer;
    private final Map<String, Integer> mapaDias = new HashMap<>();
    private CoordinadorApp coordinador;
    private ResourceBundle rb;
    private Task<List<List<Recorrido>>> currentTask;
    private static final Random generator = new Random();

    private static final Logger LOGGER = LogManager.getLogger(InterfazController.class.getName());

    private List<Parada> masterListaParadas;

    private List<String> masterListaDias = new ArrayList<>();
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="3. Initialization and Lifecycle">
    /**
     * Método de inicialización que se ejecuta automáticamente al cargar el archivo FXML.
     * * <p>Configura los elementos iniciales de la interfaz, como los toggle groups
     * y las propiedades de los componentes.</p>
     *
     * @param url no se utiliza directamente, proviene del sistema FXML.
     * @param rb recurso de texto opcional, no utilizado en esta implementación.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Agrupa los idiomas
        menuIdiomaEs.setToggleGroup(grupoIdiomas);
        menuIdiomaEn.setToggleGroup(grupoIdiomas);
        
        // Asocia la propiedad 'managed' (para que no ocupe espacio)
        // a la propiedad 'visible' del spinner.
        spinnerLoading.managedProperty().bind(spinnerLoading.visibleProperty());

        SpinnerValueFactory<Integer> horaFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spinnerHora.setValueFactory(horaFactory);

        // Configura el Spinner de Minutos (0-59), valor inicial 0, y salta de 5 en 5
        SpinnerValueFactory<Integer> minutoFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5);
        spinnerMinuto.setValueFactory(minutoFactory);
        
        // Inicializa el contenido del mapa Swing
        createAndSetSwingContent(swingNodeMapa);
        
        LOGGER.info("Controlador de interfaz inicializado correctamente.");
    }

    /**
     * Inyecta la referencia del {@link CoordinadorApp} en este controlador.
     * * <p>Este método es el punto de entrada principal para la lógica,
     * y es responsable de llamar a {@link #cargarInterfaz()} una vez
     * que el coordinador está disponible.</p>
     * * @param coordinador instancia principal que maneja la lógica de la aplicación.
     */
    @Override
    public void setCoordinador(CoordinadorApp coordinador) {
        this.coordinador = coordinador;
        // Ahora que el coordinador existe, cargamos la interfaz.
        // Esto evita un NullPointerException si se llamara desde initialize().
        cargarInterfaz();
        
        LOGGER.info("Coordinador inyectado correctamente.");

    }

    /**
     * Carga los datos iniciales en los ComboBox (días, horas, minutos y paradas).
     * * <p>Este método se invoca desde {@link #setCoordinador(CoordinadorApp)}
     * una vez que el coordinador ha sido inyectado.</p>
     */
    public void cargarInterfaz() {
        if (coordinador == null) {
            LOGGER.error("Coordinador no inicializado. No se pudo cargar la información.");
            return;
        }
        rb = coordinador.getResourceBundle();
        
        // Carga los textos iniciales (dependientes del idioma)
        actualizarTextosUI(rb);

        // Llenado de los ComboBox de Paradas
        List<Parada> paradas = new ArrayList<>(coordinador.listarParadas().values());
        cbxOrigen.setItems(FXCollections.observableArrayList(paradas));
        cbxDestino.setItems(FXCollections.observableArrayList(paradas));

        masterListaParadas = new ArrayList<>(coordinador.listarParadas().values());
        // Configura un StringConverter para mostrar la dirección de la parada
        // en lugar del 'toString()' del objeto.
        StringConverter<Parada> paradaConverter = new StringConverter<>() {
            @Override
            public String toString(Parada parada) {
                return (parada != null) ? parada.getDireccion() : "";
            }
            @Override
            public Parada fromString(String string) {
                // Cuando el usuario escribe texto y presiona Enter o pierde el foco,
                // intenta encontrar una Parada que coincida EXACTAMENTE (ignorando may/min).
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return masterListaParadas.stream()
                        .filter(p -> p.getDireccion().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null); // Si no hay coincidencia exacta, retorna null
            }
        };
        cbxOrigen.setConverter(paradaConverter);
        cbxDestino.setConverter(paradaConverter);
        setupAutoComplete(cbxOrigen, masterListaParadas, parada -> parada.getDireccion());
        setupAutoComplete(cbxDestino, masterListaParadas, parada -> parada.getDireccion());

        LOGGER.info("Interfaz cargada con datos iniciales.");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="4. Event Handlers (@FXML)">
    /**
     * Maneja el evento del botón "Calcular Recorrido".
     * * <p>Valida la entrada, prepara la UI para la carga, y lanza una
     * tarea asíncrona ({@link Task}) para calcular la ruta sin bloquear
     * la interfaz de usuario.</p>
     */
    @FXML
    private void handleCalcularRecorrido() {
        
        LOGGER.info("El usuario inició el cálculo de recorrido.");

        if (coordinador == null) {
            LOGGER.error("Coordinador no inicializado. No se puede calcular el recorrido.");
            return; 
        }
        if(!sanitizarDatosEntrada()){
            LOGGER.error("Datos de entrada no válidos.");
            return;
        }
        // 1. Obtener valores (rápido, en el FX-Thread)
        final Parada origen = cbxOrigen.getValue();
        final Parada destino = cbxDestino.getValue();
        final int dia = mapaDias.get(cbxDia.getValue());
        final LocalTime horaLlegadaParada = LocalTime.of(spinnerHora.getValue(),spinnerMinuto.getValue());

        // 2. Preparar UI: Intercambiar botones y limpiar resultados
        btnMostrarRecorrido.setVisible(false);
        btnCancelarCalculo.setVisible(true);
        contenedorRecorridos.getChildren().clear();

        // 3. Crear Pausa (Delay) para el Spinner (UX)
        // Solo muestra el spinner si la tarea tarda más de 500ms
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            prepararCarga(true); // Mostrar spinner y deshabilitar inputs
        });

        // 4. Definir la Tarea Asíncrona (Background Thread)
        currentTask = new Task<>() {
            @Override
            protected List<List<Recorrido>> call() throws Exception {
                // Simulación de carga para probar cancelación
                try {
                    Thread.sleep(3000); 
                } catch (InterruptedException e) {
                    if (isCancelled()) {
                        LOGGER.info("Cálculo de recorrido cancelado por el usuario.");
                        return null; // Salir si se cancela durante el sleep
                    }
                }

                // Chequeo final antes de la llamada pesada
                if (isCancelled()) {
                    LOGGER.info("Cálculo de recorrido cancelado por el usuario.");
                    return null;
                }
                LOGGER.info("Cálculo de recorrido iniciado.");
                // ¡La llamada pesada!
                return coordinador.calcularRecorrido(origen, destino, dia, horaLlegadaParada);
            }
        };

        // 5. Definir Callbacks (se ejecutan en el FX-Thread)
        
        currentTask.setOnSucceeded(event -> {
            pause.stop(); // Detener la pausa (evita que el spinner aparezca si ya terminó)
            
            List<List<Recorrido>> listaDeRecorridos = currentTask.getValue();
            if (listaDeRecorridos != null) {
                mostrarRecorridos(listaDeRecorridos, horaLlegadaParada);
                LOGGER.info("Cálculo de recorrido finalizado con éxito.");
            }
            
            restaurarUIAposCalculo(); // Ocultar spinner, restaurar botones/inputs
        });

        currentTask.setOnFailed(event -> {
            pause.stop();
            currentTask.getException().printStackTrace();
            mostrarAlerta("alert.title.calc_error", "alert.calc_error_body");
            restaurarUIAposCalculo();
            LOGGER.error("Error durante el cálculo de recorrido: " + currentTask.getException().getMessage());
        });

        currentTask.setOnCancelled(event -> {
            pause.stop();
            LOGGER.info("Cálculo de recorrido cancelado por el usuario.");
            restaurarUIAposCalculo();
        });

        // 6. Ejecutar la Tarea y la Pausa
        new Thread(currentTask).start();
        pause.play();
    }

    /**
     * Maneja el evento del botón "Cancelar".
     * Intenta cancelar la tarea {@code currentTask} que se está ejecutando en segundo plano.
     * @param event El evento de acción (no se utiliza).
     */
    @FXML
    private void handleCancelarCalculo(ActionEvent event) {
        if (currentTask != null && currentTask.isRunning()) {
            // true: intenta interrumpir el hilo si está en Thread.sleep()
            currentTask.cancel(true); 
        }
    }

    /**
     * Maneja el evento del botón "Limpiar Interfaz".
     * Llama al método {@link #limpiarInterfaz()} para resetear el formulario.
     */
    @FXML
    private void handleLimpiarInterfaz() {
        limpiarInterfaz();
        LOGGER.info("Interfaz limpiada por el usuario.");
    }

    /**
     * Maneja el evento del botón "Zoom In" (+).
     * Aumenta el nivel de zoom del mapa.
     */
    @FXML
    private void handleZoomIn() {
        if (mapViewer != null) {
            int zoom = mapViewer.getZoom();
            mapViewer.setZoom(Math.max(zoom - 1, 1));
        }
        LOGGER.info("Zoom In aplicado al mapa.");
    }

    /**
     * Maneja el evento del botón "Zoom Out" (-).
     * Disminuye el nivel de zoom del mapa.
     */
    @FXML
    private void handleZoomOut() {
        if (mapViewer != null) {
            int zoom = mapViewer.getZoom();
            mapViewer.setZoom(Math.min(zoom + 1, 19));
        }
        LOGGER.info("Zoom Out aplicado al mapa.");
    }

    /**
     * Maneja el cambio de idioma desde el menú de configuración.
     * Actualiza el idioma en el coordinador y refresca la UI.
     * @param event El evento de acción (no se utiliza).
     */
    @FXML
    private void handleIdiomaChange(ActionEvent event) {
        if (menuIdiomaEs.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_ES);
        } else if (menuIdiomaEn.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_EN);
        }
        LOGGER.info("Idioma cambiado a: " + coordinador.getIdiomaActual());
        actualizarIdioma();
    }

    /**
     * Maneja la acción de salir de la aplicación desde el menú.
     * @param event El evento de acción (no se utiliza).
     */
    @FXML
    private void handleSalir(ActionEvent event) {
        Platform.exit();
        System.exit(0);
        LOGGER.info("Aplicación cerrada por el usuario.");
    }

    /**
     * Maneja la acción de "Ver Logs".
     * Abre una nueva ventana y carga el contenido del archivo de log
     * de forma asíncrona.
     * @param event El evento de acción (no se utiliza).
     */
    @FXML
    private void handleVerLogs(ActionEvent event) {
        LOGGER.info("Usuario solicitó ver los logs.");
    
        // --- 1. Crear los componentes de la nueva ventana ---
        StackPane layout = new StackPane();
        layout.setPadding(new Insets(10));

        TextArea logTextArea = new TextArea();
        logTextArea.setEditable(false);
        // Usar una fuente monoespaciada es mejor para logs
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced', monospace;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(100, 100);

        layout.getChildren().addAll(logTextArea, spinner);

        // --- 2. Definir la Tarea Asíncrona para leer el archivo ---
        // (Usamos el mismo patrón que ya usas para handleCalcularRecorrido)
        Task<String> loadLogsTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    // 1. Obtener la ruta "home" del usuario
                    String userHome = System.getProperty("user.home");
                    
                    // 2. Construir la misma ruta absoluta que Log4j
                    Path logPath = Paths.get(userHome, ".colectivo-logs", "mi_app.log");
                    
                    // 3. Leer el archivo
                    return Files.readString(logPath);
                } catch (java.io.IOException e) {
                    LOGGER.error("No se pudo leer el archivo de log 'logs/mi_app.log'.", e);
                    // Pasa la excepción al onFailed
                    throw e; 
                }
            }
        };

        // --- 3. Definir Callbacks (se ejecutan en el Hilo de JavaFX) ---

        loadLogsTask.setOnRunning(e -> {
            logTextArea.setText(rb.getString("log.loading")); // "Cargando logs..."
            spinner.setVisible(true);
        });

        loadLogsTask.setOnSucceeded(e -> {
            spinner.setVisible(false);
            logTextArea.setText(loadLogsTask.getValue());
            // Mover el scroll al final para ver lo más reciente
            logTextArea.positionCaret(logTextArea.getLength()); 
        });

        loadLogsTask.setOnFailed(e -> {
            spinner.setVisible(false);
            String errorMsg = rb.getString("log.load_error"); // "Error al cargar logs"
            logTextArea.setText(errorMsg + "\n\n" + loadLogsTask.getException().getMessage());
        });

        // --- 4. Crear y mostrar la nueva ventana (Stage) ---
        Stage logStage = new Stage();
        logStage.setTitle(rb.getString("log.window_title")); // "Visor de Logs"
        // No bloquea la ventana principal
        logStage.initModality(Modality.NONE); 

        // Usamos el botón para obtener la escena, ya que sabemos que existe
        logStage.initOwner(btnMostrarRecorrido.getScene().getWindow());

        Scene scene = new Scene(layout, 700, 500); // Tamaño (ancho, alto)
        logStage.setScene(scene);
        logStage.show();

        // --- 5. Iniciar la tarea en un hilo nuevo ---
        new Thread(loadLogsTask).start();
    }

    @FXML
    private void handleDebugPintarParadas(ActionEvent event) {
        LOGGER.info("Usuario solicitó pintar todas las paradas (modo debug).");
        debugDibujarTodasLasParadas();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="5. UI Logic & Display Methods">
    /**
     * Renderiza en la interfaz los recorridos (ya calculados) devueltos por la tarea.
     *
     * <p>Para cada lista de {@link Recorrido} (un "viaje completo"), crea una "card"
     * visual con los tramos, resumen y un botón para ver en el mapa.</p>
     *
     * @param listaDeRecorridos La lista de viajes (puede ser null o vacía).
     * @param horaLlegaParada hora de llegada a la parada (usada para calcular resumen/duración).
     */
    private void mostrarRecorridos(List<List<Recorrido>> listaDeRecorridos, LocalTime horaLlegaParada) {
        
        contenedorRecorridos.getChildren().clear();

        if (listaDeRecorridos == null || listaDeRecorridos.isEmpty()) {
            mostrarCardSinResultados(contenedorRecorridos);
            return;
        }

        // Por cada "viaje completo" (que puede tener 1 o más tramos)
        for (List<Recorrido> recorridos : listaDeRecorridos) {
            VBox cardViaje = new VBox(10);
            cardViaje.setPadding(new Insets(15));
            cardViaje.setStyle("""
                -fx-background-color: #fdfdfd;
                -fx-background-radius: 14;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 8, 0, 0, 3);
            """);

            Label lblTituloCard = new Label(rb.getString("label.suggested_route"));
            lblTituloCard.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #222;");

            VBox contenedorTramos = new VBox(8);
            contenedorTramos.setFillWidth(true);

            // --- Itera por cada TRAMO (Recorrido) dentro del viaje ---
            for (Recorrido r : recorridos) {
                String linea = (r.getLinea() != null) ? r.getLinea().getNombre() : rb.getString("label.walking");
                List<Parada> paradasList = r.getParadas();
                String paradasTexto = paradasList.stream()
                        .map(Parada::getDireccion)
                        .collect(Collectors.joining(" → "));

                String paradas = rb.getString("label.stops") + " " + paradasTexto;
                String hora = rb.getString("label.departure_time") + " " + r.getHoraSalida();
                String duracion = rb.getString("label.duration") + " " + Tiempo.segundosATiempo(r.getDuracion());
                String color = generarColorAleatorio();

                VBox cardTramo = new VBox(5);
                cardTramo.setPadding(new Insets(10));
                cardTramo.setStyle("""
                    -fx-background-color: #ffffff;
                    -fx-background-radius: 10;
                    -fx-border-color: #ddd;
                    -fx-border-radius: 10;
                """);
                cardTramo.setMaxWidth(Double.MAX_VALUE);

                Label lblLinea = new Label(linea);
                lblLinea.setStyle("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-weight: bold;".formatted(color));
                lblLinea.setWrapText(true);

                Label lblParadas = new Label(paradas);
                lblParadas.setStyle("-fx-text-fill: #333;");
                lblParadas.setWrapText(true);
                lblParadas.setMaxWidth(300);

                Label lblHora = new Label(hora);
                lblHora.setStyle("-fx-text-fill: #666;");
                lblHora.setWrapText(true);

                Label lblDuracion = new Label(duracion);
                lblDuracion.setStyle("-fx-text-fill: #666;");
                lblDuracion.setWrapText(true);

                cardTramo.getChildren().addAll(lblLinea, lblParadas, lblHora, lblDuracion);
                contenedorTramos.getChildren().add(cardTramo);
            }

            // --- Resumen final del VIAJE COMPLETO ---
            VBox cardResumen = new VBox(5);
            cardResumen.setPadding(new Insets(10));
            cardResumen.setStyle("""
                -fx-background-color: #f9f9f9;
                -fx-background-radius: 10;
                -fx-border-color: #ccc;
                -fx-border-radius: 10;
            """);

            Recorrido ultimo = recorridos.get(recorridos.size() - 1);
            String duracionTotal = rb.getString("label.total_duration") + " " + Tiempo.calcularDuracionTotalViaje(ultimo, horaLlegaParada);
            String horaLlegada = rb.getString("label.arrival_time") + " " + Tiempo.calcularHoraLlegadaDestino(ultimo);

            Label lblDuracionTotal = new Label(duracionTotal);
            lblDuracionTotal.setStyle("-fx-text-fill: #222; -fx-font-weight: bold; -fx-font-size: 14;");

            Label lblHoraLlegada = new Label(horaLlegada);
            lblHoraLlegada.setStyle("-fx-text-fill: #333; -fx-font-size: 13;");

            cardResumen.getChildren().addAll(lblDuracionTotal, lblHoraLlegada);

            // --- Botón para ver en Mapa ---
            Button btnVerRecorrido = new Button(rb.getString("button.view_route"));
            btnVerRecorrido.setStyle("""
                -fx-background-color: #4ECDC4;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 8 16;
            """);
            btnVerRecorrido.setOnAction(e -> {
                LOGGER.info("Usuario solicitó ver recorrido en el mapa.");
                dibujarRecorrido(recorridos);
            });

            cardViaje.getChildren().addAll(lblTituloCard, contenedorTramos, cardResumen, btnVerRecorrido);
            contenedorRecorridos.getChildren().add(cardViaje);
            LOGGER.info("Recorrido mostrado en la interfaz.");
        }
    }

    /**
     * Alterna la vista al modo "cargando".
     * Muestra el spinner y deshabilita los controles del formulario.
     *
     * @param cargando true para mostrar el spinner y deshabilitar inputs, false para lo contrario.
     */
    private void prepararCarga(boolean cargando) {
        spinnerLoading.setVisible(cargando);
        contenedorRecorridos.setVisible(!cargando);

        // Deshabilita los controles del formulario
        btnLimpiarInterfaz.setDisable(cargando);
        cbxDia.setDisable(cargando);
        cbxOrigen.setDisable(cargando);
        cbxDestino.setDisable(cargando);
        spinnerHora.setDisable(cargando);
        spinnerMinuto.setDisable(cargando);
    }

    /**
     * Restaura la UI a su estado inicial después de que un cálculo
     * termine (por éxito, fallo o cancelación).
     */
    private void restaurarUIAposCalculo() {
        // 1. Oculta el spinner (si se mostró) y habilita controles
        prepararCarga(false);

        // 2. Restaura los botones
        btnMostrarRecorrido.setVisible(true);
        btnCancelarCalculo.setVisible(false);
    }

    /**
     * Recarga todos los textos de la UI usando el ResourceBundle actualizado.
     * Se llama al cargar la interfaz y al cambiar de idioma.
     */
    public void actualizarIdioma() {
        if (coordinador == null || coordinador.getResourceBundle() == null) {
            LOGGER.error("⚠️ No se pudo actualizar el idioma: coordinador o configuración nulos");
            return;
        }
        rb = coordinador.getResourceBundle();
        limpiarInterfaz();
        actualizarTextosUI(rb);
        LOGGER.info("Interfaz actualizada al idioma: " + coordinador.getIdiomaActual());
    }

    /**
     * Método helper que actualiza todos los componentes de texto de la UI.
     * @param bundle El ResourceBundle del idioma seleccionado.
     */
    private void actualizarTextosUI(ResourceBundle bundle) {
        // Labels principales
        lblTitulo.setText(bundle.getString("app.title"));
        lblDia.setText(bundle.getString("label.day") + ":");
        lblOrigen.setText(bundle.getString("label.origin") + ":");
        lblDestino.setText(bundle.getString("label.destination") + ":");
        lblHorario.setText(bundle.getString("label.time") + ":");
        lblRutasDisponibles.setText(bundle.getString("label.available_routes"));

        // Botones
        btnMostrarRecorrido.setText(bundle.getString("button.show_routes"));
        btnCancelarCalculo.setText(bundle.getString("button.cancel"));
        btnLimpiarInterfaz.setText(bundle.getString("button.clear"));

        // ComboBox placeholders (prompts)
        cbxDia.setPromptText(bundle.getString("label.day"));
        cbxOrigen.setPromptText(bundle.getString("label.origin"));
        cbxDestino.setPromptText(bundle.getString("label.destination"));
        // cbxHora.setPromptText(bundle.getString("prompt.hour"));
        // cbxMinuto.setPromptText(bundle.getString("prompt.minute"));

        // Menu superior
        menuConfig.setText(bundle.getString("menu.config"));
        menuConfigIdioma.setText(bundle.getString("menu.config.language"));
        menuIdiomaEs.setText(bundle.getString("menu.config.lang.es"));
        menuIdiomaEn.setText(bundle.getString("menu.config.lang.en"));
        menuConfigSalir.setText(bundle.getString("menu.config.exit"));
        menuMostrarParadas.setText(bundle.getString("menu.debug.paint_stops"));

        // Actualiza el mapa de días y el ComboBox de días
        mapaDias.clear();
        masterListaDias.clear();

        String diaLunes = bundle.getString("cbx.day.monday");
        mapaDias.put(diaLunes, 1);
        masterListaDias.add(diaLunes);
        
        String diaMartes = bundle.getString("cbx.day.tuesday");
        mapaDias.put(diaMartes, 2);
        masterListaDias.add(diaMartes);

        String diaMiercoles = bundle.getString("cbx.day.wednesday");
        mapaDias.put(diaMiercoles, 3);
        masterListaDias.add(diaMiercoles);

        String diaJueves = bundle.getString("cbx.day.thursday");
        mapaDias.put(diaJueves, 4);
        masterListaDias.add(diaJueves);

        String diaViernes = bundle.getString("cbx.day.friday");
        mapaDias.put(diaViernes, 5);
        masterListaDias.add(diaViernes);

        String diaSabado = bundle.getString("cbx.day.saturday");
        mapaDias.put(diaSabado, 6);
        masterListaDias.add(diaSabado);

        String diaDomingo = bundle.getString("cbx.day.sunday");
        mapaDias.put(diaDomingo, 7);
        masterListaDias.add(diaDomingo);

        cbxDia.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String dia) {
                return dia; // String a String
            }

            @Override
            public String fromString(String string) {
                // Busca si el texto escrito es un día válido
                return masterListaDias.stream()
                        .filter(dia -> dia.equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null); // Si no, es inválido
            }
        });

        // 4. Llama a la nueva función de setup
        setupAutoComplete(cbxDia, masterListaDias, dia -> dia);
    }
    
    /**
     * Resetea todos los campos de entrada y resultados de la UI.
     */
    private void limpiarInterfaz() {
        contenedorRecorridos.getChildren().clear();
        // Asigna null para que se muestre el prompt text
        cbxDia.getSelectionModel().clearSelection();
        cbxOrigen.getSelectionModel().clearSelection();
        cbxDestino.getSelectionModel().clearSelection();
        spinnerHora.getValueFactory().setValue(12);
        spinnerMinuto.getValueFactory().setValue(0);
        mapViewer.setOverlayPainter(null);
    }

    /**
     * Muestra un mensaje en la interfaz cuando no existen recorridos disponibles.
     * @param contenedorDeRecorridos El VBox donde se insertará el mensaje.
     */
    private void mostrarCardSinResultados(VBox contenedorDeRecorridos){
        if(rb == null) {
            rb = coordinador.getResourceBundle();
        }
        VBox cardSinResultados = new VBox();
        cardSinResultados.setPadding(new Insets(40));
        cardSinResultados.setAlignment(Pos.CENTER);
        cardSinResultados.setStyle("""
            -fx-background-color: #fdfdfd;
            -fx-background-radius: 14;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3);
        """);

        Label lblMensaje = new Label(rb.getString("no_routes.title"));
        lblMensaje.setStyle("""
            -fx-font-size: 18;
            -fx-font-family: Helvetica;
            -fx-text-fill: #666;
            -fx-font-weight: bold;
        """);

        Label lblSugerencia = new Label(rb.getString("no_routes.subtitle"));
        lblSugerencia.setStyle("""
            -fx-font-size: 14;
            -fx-font-family: Helvetica;
            -fx-text-fill: #999;
        """);

        cardSinResultados.getChildren().addAll(lblMensaje, lblSugerencia);
        contenedorDeRecorridos.getChildren().add(cardSinResultados);
        LOGGER.info("Se mostró el mensaje de 'sin resultados' en la interfaz.");
    }

    /**
     * Muestra una ventana de alerta de error con un título y mensaje.
     * @param titulo Clave del ResourceBundle para el título.
     * @param mensaje Clave del ResourceBundle para el contenido.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(rb.getString(titulo));
        alert.setHeaderText(null);
        alert.setContentText(rb.getString(mensaje));
        alert.showAndWait();
        LOGGER.error("Se mostró una alerta: " + rb.getString(titulo));
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="6. Validation">
    /**
     * Verifica y normaliza los datos ingresados por el usuario.
     * * <p>Controla que los campos estén completos, que las paradas de origen y destino
     * sean distintas, y que la hora ingresada sea válida.</p>
     * * @return true si los datos son válidos, false si hay errores.
     */
    private boolean sanitizarDatosEntrada() {
        String diaStr = cbxDia.getValue() != null ? cbxDia.getValue().trim() : null;
        Parada origen = cbxOrigen.getValue();
        Parada destino = cbxDestino.getValue();
        

        if (diaStr == null || diaStr.isEmpty() || origen == null || destino == null) {
            mostrarAlerta("alert.title.incomplete", "alert.incomplete");
            return false;
        }
        if (origen.equals(destino)) {
            mostrarAlerta("alert.title.invalid_data", "alert.invalid_stops");
            return false;
        }
        
        return true;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="7. Swing/Map Integration">
    /**
     * Crea y configura el componente Swing que muestra el mapa de OpenStreetMap.
     * * <p>Este método inicializa el {@link JXMapViewer} dentro de un {@link SwingNode},
     * configurando los eventos de movimiento y zoom.</p>
     * * @param swingNode nodo JavaFX que contendrá el componente Swing.
     */
    private void createAndSetSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            TileFactoryInfo info = new TileFactoryInfo(0, 17, 17, 256, true, true,
                "https://tile.openstreetmap.org", "x", "y", "z") {
                @Override
                public String getTileUrl(int x, int y, int zoom) {
                    int z = 17 - zoom;
                    return String.format("%s/%d/%d/%d.png", getBaseURL(), z, x, y);
                }
            };

            DefaultTileFactory tileFactory = new DefaultTileFactory(info);
            mapViewer = new JXMapViewer();

            PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
            mapViewer.addMouseListener(panListener);
            mapViewer.addMouseMotionListener(panListener);
            mapViewer.addMouseWheelListener(new org.jxmapviewer.input.ZoomMouseWheelListenerCenter(mapViewer));

            mapViewer.setTileFactory(tileFactory);
            GeoPosition puertoMadryn = new GeoPosition(coordinador.getOrigenLatitud(), coordinador.getOrigenLongitud());
            mapViewer.setZoom(coordinador.getZoom());
            mapViewer.setAddressLocation(puertoMadryn);

            swingNode.setContent(mapViewer);
        });
        LOGGER.info("Mapa SwingNode inicializado correctamente.");
        
    }

    /**
     * Dibuja visualmente el recorrido seleccionado sobre el mapa.
     * * <p>Convierte las paradas en coordenadas geográficas y las traza en el {@link JXMapViewer}.</p>
     * * @param recorridos lista de recorridos que conforman un viaje completo.
     */
    private void dibujarRecorrido(List<Recorrido> recorridos) {
        List<GeoPosition> paradasGeo = new ArrayList<>();
        for (Recorrido r : recorridos) {
            r.getParadas().forEach(p -> {
                GeoPosition pos = new GeoPosition(p.getLatitud(), p.getLongitud());
                if (paradasGeo.isEmpty() || !paradasGeo.get(paradasGeo.size() - 1).equals(pos)) {
                    paradasGeo.add(pos);
                }
            });
        }
        MapPainter pintor = new MapPainter(paradasGeo, java.awt.Color.RED);
        mapViewer.setOverlayPainter(pintor);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="8. Utility Methods">
    /**
     * Genera un color pastel aleatorio en formato hexadecimal.
     * * <p>Se usa para darle una identidad visual distinta a cada tramo de recorrido.</p>
     * * @return color en formato hexadecimal (ej. "#A4D3EE").
     */
    private String generarColorAleatorio() {
        float hue = generator.nextFloat();
        float saturation = 0.25f + (generator.nextFloat() * 0.20f);
        float brightness = 0.85f + (generator.nextFloat() * 0.10f);
        java.awt.Color color = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
    //</editor-fold>

    /**
     * [DEBUG] Dibuja *todas* las paradas conocidas por el coordinador,
     * coloreando cada línea con un color distinto.
     * <p>Las paradas que no pertenecen a ninguna línea se verán en GRIS.</p>
     */
    public void debugDibujarTodasLasParadas() {
        if (coordinador == null) {
            LOGGER.warn("Debug: Coordinador nulo, no se pueden dibujar paradas.");
            return;
        }
        if (mapViewer == null) {
            LOGGER.warn("Debug: MapViewer nulo, no se pueden dibujar paradas.");
            return;
        }

        LOGGER.info("Iniciando dibujado de debug: coloreando paradas por línea.");

        // 1. Lista para guardar todos los painters (uno por cada capa)
        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        // 2. CAPA BASE: Dibujamos TODAS las paradas primero en color GRIS.
        // Usamos un Set para que no haya duplicados
        Set<GeoPosition> todasLasParadasGeo = new HashSet<>();
        for (Parada p : coordinador.listarParadas().values()) {
            todasLasParadasGeo.add(new GeoPosition(p.getLatitud(), p.getLongitud()));
        }
        
        // Creamos un painter base con todas las paradas en gris
        ParadasDebugPainter painterBase = new ParadasDebugPainter(
            todasLasParadasGeo, 
            java.awt.Color.GRAY
        );
        painters.add(painterBase);


        // 3. CAPAS DE LÍNEA: Iteramos por cada línea, como sugeriste
        for (Linea linea : coordinador.listarLineas().values()) {
            
            // 4. Obtenemos las paradas de ESTA línea
            Collection<GeoPosition> paradasDeLinea = new ArrayList<>();
            if (linea.getParadas() != null) {
                for (Parada p : linea.getParadas()) {
                    paradasDeLinea.add(new GeoPosition(p.getLatitud(), p.getLongitud()));
                }
            }

            if (!paradasDeLinea.isEmpty()) {
                // 5. Generamos un color aleatorio para esta línea
                java.awt.Color colorLinea = java.awt.Color.decode(generarColorAleatorio());

                // 6. Creamos un painter específico para esta línea con su color
                ParadasDebugPainter painterLinea = new ParadasDebugPainter(
                    paradasDeLinea, 
                    colorLinea
                );
                
                // 7. Lo agregamos a la lista
                // (Se dibujará "encima" de la capa base gris)
                painters.add(painterLinea);
            }
        }

        // 8. Creamos un CompoundPainter que combine todos los painters de la lista
        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);

        // 9. Asignamos el CompoundPainter al mapa
        mapViewer.setOverlayPainter(compoundPainter);
        LOGGER.info("Debug: {} painters (1 base + {} líneas) dibujados en el mapa.",
                painters.size(), painters.size() - 1);
    }
    
    /**
     * Configura un ComboBox editable (genérico) para que funcione como un
     * campo de autocompletado/búsqueda.
     *
     * @param <T> El tipo de objeto en el ComboBox (ej. Parada, String).
     * @param comboBox El ComboBox (editable) a configurar.
     * @param masterList La lista completa de items para filtrar.
     * @param stringExtractor Una función que toma un objeto <T> y devuelve
     * el String por el cual se debe buscar.
     */
    private <T> void setupAutoComplete(ComboBox<T> comboBox, List<T> masterList, 
                                    Function<T, String> stringExtractor) {
        
        // 1. Establece la lista inicial
        comboBox.setItems(FXCollections.observableArrayList(masterList));

        // 2. Listener para filtrar la lista mientras el usuario escribe
        comboBox.getEditor().setOnKeyReleased(event -> {
            String typedText = comboBox.getEditor().getText();

            if (typedText == null || typedText.isEmpty()) {
                comboBox.setItems(FXCollections.observableArrayList(masterList));
                comboBox.hide(); 
            } else {
                // Filtra la lista maestra usando el extractor genérico
                List<T> filteredList = masterList.stream()
                        .filter(item -> 
                            // Aquí usamos la función que pasamos como parámetro
                            stringExtractor.apply(item).toLowerCase()
                                            .contains(typedText.toLowerCase())
                        )
                        .collect(Collectors.toList());

                comboBox.setItems(FXCollections.observableArrayList(filteredList));
                comboBox.show(); // Muestra los resultados
            }
        });

        // 3. Listener para validar/limpiar cuando se pierde el foco
        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Si pierde el foco
                String currentText = comboBox.getEditor().getText();

                // Busca un item válido usando el extractor
                T validItem = masterList.stream()
                        .filter(item -> stringExtractor.apply(item).equalsIgnoreCase(currentText))
                        .findFirst()
                        .orElse(null);

                if (validItem != null) {
                    // Texto válido. Forzamos el valor (esto dispara el converter)
                    comboBox.setValue(validItem);
                    // Nos aseguramos que el texto del editor tenga el formato correcto
                    comboBox.getEditor().setText(stringExtractor.apply(validItem));
                } else {
                    // Texto inválido, limpia todo
                    comboBox.setValue(null);
                    comboBox.getEditor().setText(null);
                }
            }
        });

        // 4. Previene que la rueda del mouse cambie la selección
        comboBox.addEventFilter(ScrollEvent.ANY, event -> {
            if (!comboBox.isShowing()) {
                event.consume();
            }
        });
    }
}