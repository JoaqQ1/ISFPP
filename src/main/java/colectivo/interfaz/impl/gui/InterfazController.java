package colectivo.interfaz.impl.gui;

import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import colectivo.controlador.Constantes;
import colectivo.controlador.CoordinadorApp;
import colectivo.coordinador.Coordinable;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

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
    @FXML private ComboBox<String> cbxHora;
    @FXML private ComboBox<String> cbxMinuto;

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
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="2. Private Fields">
    private JXMapViewer mapViewer;
    private final Map<String, Integer> mapaDias = new HashMap<>();
    private CoordinadorApp coordinador;
    private ResourceBundle rb;
    private Task<List<List<Recorrido>>> currentTask;
    private static final Random generator = new Random();
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

        // Inicializa el contenido del mapa Swing
        createAndSetSwingContent(swingNodeMapa);
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
    }

    /**
     * Carga los datos iniciales en los ComboBox (días, horas, minutos y paradas).
     * * <p>Este método se invoca desde {@link #setCoordinador(CoordinadorApp)}
     * una vez que el coordinador ha sido inyectado.</p>
     */
    public void cargarInterfaz() {
        if (coordinador == null) {
            System.err.println("Coordinador no inicializado. No se pudo cargar la información.");
            return;
        }
        rb = coordinador.getResourceBundle();
        
        // Carga los textos iniciales (dependientes del idioma)
        actualizarTextosUI(rb);

        // Llenado de los ComboBox de Hora/Minuto
        List<String> horas = new ArrayList<>();
        List<String> minutos = new ArrayList<>();
        for (int h = 1; h <= 22; h++) horas.add(String.format("%02d", h));
        for (int m = 0; m <= 59; m++) minutos.add(String.format("%02d", m));

        cbxHora.setItems(FXCollections.observableArrayList(horas));
        cbxMinuto.setItems(FXCollections.observableArrayList(minutos));

        // Llenado de los ComboBox de Paradas
        List<Parada> paradas = new ArrayList<>(coordinador.listarParadas().values());
        cbxOrigen.setItems(FXCollections.observableArrayList(paradas));
        cbxDestino.setItems(FXCollections.observableArrayList(paradas));

        // Configura un StringConverter para mostrar la dirección de la parada
        // en lugar del 'toString()' del objeto.
        StringConverter<Parada> paradaConverter = new StringConverter<>() {
            @Override
            public String toString(Parada parada) {
                return (parada != null) ? parada.getDireccion() : "";
            }
            @Override
            public Parada fromString(String string) {
                return null; // No se necesita conversión inversa
            }
        };
        cbxOrigen.setConverter(paradaConverter);
        cbxDestino.setConverter(paradaConverter);
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
        if (coordinador == null || !sanitizarDatosEntrada()) {
            return; 
        }

        // 1. Obtener valores (rápido, en el FX-Thread)
        final Parada origen = cbxOrigen.getValue();
        final Parada destino = cbxDestino.getValue();
        final int dia = mapaDias.get(cbxDia.getValue());
        final LocalTime horaLlegadaParada = LocalTime.parse(cbxHora.getValue() + ":" + cbxMinuto.getValue());

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
                        return null; // Salir si se cancela durante el sleep
                    }
                }

                // Chequeo final antes de la llamada pesada
                if (isCancelled()) {
                    return null;
                }

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
            }
            
            restaurarUIAposCalculo(); // Ocultar spinner, restaurar botones/inputs
        });

        currentTask.setOnFailed(event -> {
            pause.stop();
            currentTask.getException().printStackTrace();
            mostrarAlerta("alert.title.calc_error", "alert.calc_error_body");
            restaurarUIAposCalculo();
        });

        currentTask.setOnCancelled(event -> {
            pause.stop();
            System.out.println("Cálculo cancelado por el usuario.");
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
            btnVerRecorrido.setOnAction(e -> dibujarRecorrido(recorridos));

            cardViaje.getChildren().addAll(lblTituloCard, contenedorTramos, cardResumen, btnVerRecorrido);
            contenedorRecorridos.getChildren().add(cardViaje);
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
        cbxHora.setDisable(cargando);
        cbxMinuto.setDisable(cargando);
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
            System.err.println("⚠️ No se pudo actualizar el idioma: coordinador o configuración nulos");
            return;
        }
        rb = coordinador.getResourceBundle();
        actualizarTextosUI(rb);
        contenedorRecorridos.getChildren().clear();
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
        cbxHora.setPromptText(bundle.getString("prompt.hour"));
        cbxMinuto.setPromptText(bundle.getString("prompt.minute"));

        // Menu superior
        menuConfig.setText(bundle.getString("menu.config"));
        menuConfigIdioma.setText(bundle.getString("menu.config.language"));
        menuIdiomaEs.setText(bundle.getString("menu.config.lang.es"));
        menuIdiomaEn.setText(bundle.getString("menu.config.lang.en"));
        menuConfigSalir.setText(bundle.getString("menu.config.exit"));

        // Actualiza el mapa de días y el ComboBox de días
        mapaDias.clear();
        mapaDias.put(bundle.getString("cbx.day.monday"), 1);
        mapaDias.put(bundle.getString("cbx.day.tuesday"), 2);
        mapaDias.put(bundle.getString("cbx.day.wednesday"), 3);
        mapaDias.put(bundle.getString("cbx.day.thursday"), 4);
        mapaDias.put(bundle.getString("cbx.day.friday"), 5);
        mapaDias.put(bundle.getString("cbx.day.saturday"), 6);
        mapaDias.put(bundle.getString("cbx.day.sunday"), 7);

        cbxDia.getItems().clear();
        cbxDia.getItems().addAll(
            bundle.getString("cbx.day.monday"),
            bundle.getString("cbx.day.tuesday"),
            bundle.getString("cbx.day.wednesday"),
            bundle.getString("cbx.day.thursday"),
            bundle.getString("cbx.day.friday"),
            bundle.getString("cbx.day.saturday"),
            bundle.getString("cbx.day.sunday")
        );
    }
    
    /**
     * Resetea todos los campos de entrada y resultados de la UI.
     */
    private void limpiarInterfaz() {
        contenedorRecorridos.getChildren().clear();
        cbxDia.getSelectionModel().clearSelection();
        cbxOrigen.getSelectionModel().clearSelection();
        cbxDestino.getSelectionModel().clearSelection();
        cbxHora.getSelectionModel().clearSelection();
        cbxMinuto.getSelectionModel().clearSelection();
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
        String horaStr = cbxHora.getValue() != null ? cbxHora.getValue().trim() : null;
        String minutoStr = cbxMinuto.getValue() != null ? cbxMinuto.getValue().trim() : null;

        if (diaStr == null || diaStr.isEmpty() || origen == null || destino == null ||
            horaStr == null || minutoStr == null) {
            mostrarAlerta("alert.title.incomplete", "alert.incomplete");
            return false;
        }
        if (origen.equals(destino)) {
            mostrarAlerta("alert.title.invalid_data", "alert.invalid_stops");
            return false;
        }
        try {
            int hora = Integer.parseInt(horaStr);
            int minuto = Integer.parseInt(minutoStr);
            if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
                mostrarAlerta("alert.title.invalid_time", "alert.invalid_time");
                return false;
            }
            cbxHora.setValue(String.format("%02d", hora));
            cbxMinuto.setValue(String.format("%02d", minuto));
        } catch (NumberFormatException e) {
            mostrarAlerta("alert.title.invalid_format", "alert.invalid_format_body");
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
            GeoPosition puertoMadryn = new GeoPosition(-42.7692, -65.0385);
            mapViewer.setZoom(4);
            mapViewer.setAddressLocation(puertoMadryn);

            swingNode.setContent(mapViewer);
        });
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
}