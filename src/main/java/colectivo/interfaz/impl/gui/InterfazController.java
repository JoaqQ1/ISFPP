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
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controlador principal de la interfaz gráfica de usuario (GUI) de la aplicación de colectivos.
 * 
 * <p>Esta clase conecta los elementos visuales del FXML con la lógica del programa, permitiendo
 * al usuario seleccionar una parada de origen, destino, día y hora para calcular posibles recorridos.</p>
 * 
 * <p>También administra la integración del mapa (usando Swing dentro de JavaFX) y se comunica
 * con el objeto {@link CoordinadorApp}, que maneja la lógica del dominio.</p>
 */
public class InterfazController implements Initializable, Coordinable {

    // --- Elementos de la interfaz definidos en el archivo FXML ---
    @FXML private AnchorPane anchorPaneFrmPrincipal;
    @FXML private VBox vboxResultados;
    @FXML private VBox contenedorRecorridos;
    @FXML private Label lblDestino;
    @FXML private Label lblDia;
    @FXML private Label lblHorario;
    @FXML private Label lblOrigen;
    @FXML private Label lblTitulo;
    @FXML private Label lblRutasDisponibles;
    @FXML private ComboBox<String> cbxDia;
    @FXML private ComboBox<Parada> cbxOrigen;
    @FXML private ComboBox<Parada> cbxDestino;
    @FXML private ComboBox<String> cbxHora;
    @FXML private ComboBox<String> cbxMinuto;
    @FXML private Button btnMostrarRecorrido; 
    @FXML private Button btnLimpiarInterfaz;
    @FXML private Button btnZoomIn, btnZoomOut;
    @FXML private SwingNode swingNodeMapa;

    //====================================================
    @FXML private ToggleGroup grupoIdiomas;
    @FXML private RadioMenuItem menuIdiomaEs;
    @FXML private RadioMenuItem menuIdiomaEn;

    // Grupo de Tipos de Archivo
    // @FXML private ToggleGroup grupoArchivos;
    // @FXML private RadioMenuItem menuArchivoJson;
    // @FXML private RadioMenuItem menuArchivoXml;

    // --- Elementos del Menú ---
    @FXML private Menu menuConfig;
    @FXML private Menu menuConfigIdioma;
    @FXML private MenuItem menuConfigSalir;
    
    //====================================================

    private JXMapViewer mapViewer;
    private final Map<String, Integer> mapaDias = new HashMap<>();
    private CoordinadorApp coordinador;
    private static Random generator = new Random();
    ResourceBundle rb;

    /**
     * Inyecta la referencia del {@link CoordinadorApp} en este controlador.
     * 
     * <p>Esto permite que la interfaz gráfica acceda a los métodos de negocio y datos
     * del sistema (por ejemplo, listar paradas o calcular recorridos).</p>
     * 
     * @param coordinador instancia principal de Coordinador que maneja la lógica de la aplicación.
     */
    @Override
    public void setCoordinador(CoordinadorApp coordinador) {
        this.coordinador = coordinador;
    }

    /**
     * Método de inicialización que se ejecuta automáticamente al cargar el archivo FXML.
     * 
     * <p>Aquí se configuran los elementos iniciales de la interfaz, como los combos y el mapa base.</p>
     *
     * @param url no se utiliza directamente, proviene del sistema FXML.
     * @param rb recurso de texto opcional, no utilizado en esta implementación.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Agrupa los idiomas
        menuIdiomaEs.setToggleGroup(grupoIdiomas);
        menuIdiomaEn.setToggleGroup(grupoIdiomas);
        
        // Agrupa los tipos de archivo
        // menuArchivoJson.setToggleGroup(grupoArchivos);
        // menuArchivoXml.setToggleGroup(grupoArchivos);

        cargarInterfaz();
        createAndSetSwingContent(swingNodeMapa);
    }

    /**
     * Carga los datos iniciales en los ComboBox (días, horas, minutos y paradas).
     * 
     * <p>Este método se invoca una vez que el Coordinador ha sido inyectado, ya que
     * necesita acceder a la lista de paradas del sistema.</p>
     */
    public void cargarInterfaz() {
        if (coordinador == null) {
            System.err.println("Coordinador no inicializado. No se pudo cargar la información.");
            return;
        }
        rb = coordinador.getResourceBundle();
        
        mapaDias.put(rb.getString("cbx.day.monday"),1);
        mapaDias.put(rb.getString("cbx.day.tuesday"),2);
        mapaDias.put(rb.getString("cbx.day.wednesday"),3);
        mapaDias.put(rb.getString("cbx.day.thursday"),4);
        mapaDias.put(rb.getString("cbx.day.friday"),5);
        mapaDias.put(rb.getString("cbx.day.saturday"),6);
        mapaDias.put(rb.getString("cbx.day.sunday"),7);

        // Llenado de los ComboBox
        List<String> dias = List.of(rb.getString("cbx.day.monday"), rb.getString("cbx.day.tuesday"), rb.getString("cbx.day.wednesday"), rb.getString("cbx.day.thursday"), rb.getString("cbx.day.friday"), rb.getString("cbx.day.saturday"), rb.getString("cbx.day.sunday"));
        cbxDia.setItems(FXCollections.observableArrayList(dias));


        cbxDia.setPromptText(rb.getString("label.day"));
        cbxOrigen.setPromptText(rb.getString("label.origin"));
        cbxDestino.setPromptText(rb.getString("label.destination"));

        cbxHora.setPromptText(rb.getString("prompt.hour"));
        cbxMinuto.setPromptText(rb.getString("prompt.minute"));

        List<String> horas = new ArrayList<>();
        List<String> minutos = new ArrayList<>();
        for (int h = 1; h <= 22; h++) horas.add(String.format("%02d", h));
        for (int m = 0; m <= 59; m++) minutos.add(String.format("%02d", m));

        cbxHora.setItems(FXCollections.observableArrayList(horas));
        cbxMinuto.setItems(FXCollections.observableArrayList(minutos));

        List<Parada> paradas = new ArrayList<>(coordinador.listarParadas().values());
        cbxOrigen.setItems(FXCollections.observableArrayList(paradas));
        cbxDestino.setItems(FXCollections.observableArrayList(paradas));

        // Muestra la dirección de la parada en lugar de su representación de objeto
        StringConverter<Parada> paradaConverter = new StringConverter<>() {
            @Override
            public String toString(Parada parada) {
                return (parada != null) ? parada.getDireccion() : "";
            }
            @Override
            public Parada fromString(String string) {
                return null;
            }
        };
        cbxOrigen.setConverter(paradaConverter);
        cbxDestino.setConverter(paradaConverter);
    }

    /**
     * Maneja el evento del botón "Calcular Recorrido".
     * 
     * <p>Verifica que todos los datos sean válidos y luego invoca al Coordinador
     * para obtener las posibles rutas entre el origen y el destino seleccionados.</p>
     */
    @FXML
    private void handleCalcularRecorrido() {
        if (coordinador == null) {
            mostrarAlerta("Error de Configuración", "El coordinador no ha sido inicializado.");
            return;
        }
        if (!sanitizarDatosEntrada()) return;

        try {
            String diaStr = cbxDia.getValue();
            Parada origen = cbxOrigen.getValue();
            Parada destino = cbxDestino.getValue();
            String horaStr = cbxHora.getValue();
            String minutoStr = cbxMinuto.getValue();
            
            int dia = mapaDias.get(diaStr);
            LocalTime horaLlegadaParada = LocalTime.parse(horaStr + ":" + minutoStr);
            
            mostrarRecorridos(origen, destino, dia, horaLlegadaParada);
        } catch (Exception e) {
            mostrarAlerta("alert.title.calc_error", "alert.calc_error_body");
            e.printStackTrace();
        }
    }

    /**
     * Renderiza en la interfaz los recorridos devueltos por el coordinador.
     *
     * <p>Para cada lista de {@link Recorrido} (que representa un "viaje completo")
     * crea una "card" visual que contiene los tramos del viaje, un resumen con
     * duración y hora de llegada, y un botón para ver el recorrido en el mapa.</p>
     *
     * <p>Si la lista está vacía o es null, delega en {@link #mostrarCardSinResultados(VBox)}</p>
     *
     * @param origen parada de origen seleccionada (se utiliza solo para contexto en la UI).
     * @param destino parada de destino seleccionada (se utiliza solo para contexto en la UI).
     * @param dia     día (como entero según el mapa de días) para la consulta al coordinador.
     * @param horaLlegaParada hora de llegada a la parada (usada para calcular resumen/duración).
     */
    private void mostrarRecorridos(Parada origen, Parada destino, int dia, LocalTime horaLlegaParada) {
        
        contenedorRecorridos.getChildren().clear();

        // Solicita al coordinador las alternativas de recorridos con los datos seleccionados
        List<List<Recorrido>> listaDeRecorridos = coordinador.calcularRecorrido(origen, destino, dia, horaLlegaParada);

        // Si no hay resultados, mostramos la card de "sin resultados"
        if (listaDeRecorridos == null || listaDeRecorridos.isEmpty()) {
            mostrarCardSinResultados(contenedorRecorridos);
            return;
        }

        // Por cada "viaje completo" que contenga múltiples tramos (Recorrido)
        for (List<Recorrido> recorridos : listaDeRecorridos) {
            // Card contenedora de un "viaje completo"
            VBox cardViaje = new VBox(10);
            cardViaje.setPadding(new Insets(15));
            cardViaje.setStyle("""
                -fx-background-color: #fdfdfd;
                -fx-background-radius: 14;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 8, 0, 0, 3);
            """);

            // Título de bloque
            Label lblTitulo = new Label(rb.getString("label.suggested_route"));
            lblTitulo.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #222;");

            VBox contenedorTramos = new VBox(8);
            contenedorTramos.setFillWidth(true);

            // Para cada tramo del viaje creamos una tarjeta con sus datos
            for (Recorrido r : recorridos) {
                String linea = (r.getLinea() != null) ? r.getLinea().getNombre() : rb.getString("label.walking");

                // Construimos texto de paradas separadas por flecha
                List<Parada> paradasList = r.getParadas();
                String paradasTexto = paradasList.stream()
                        .map(Parada::getDireccion)
                        .collect(Collectors.joining(" → "));

                String paradas = rb.getString("label.stops") + " " + paradasTexto;
                String hora = rb.getString("label.departure_time") + " " + r.getHoraSalida();
                String duracion = rb.getString("label.duration") + " " + Tiempo.segundosATiempo(r.getDuracion());

                // Color aleatorio para identificar visualmente el tramo
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

            // Resumen final del viaje (usado el último tramo para calcular llegada/duración total)
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

            // Botón que permite ver el recorrido en el mapa (delegando a dibujarRecorrido)
            Button btnVerRecorrido = new Button(rb.getString("button.view_route"));
            btnVerRecorrido.setStyle("""
                -fx-background-color: #4ECDC4;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-padding: 8 16;
            """);

            btnVerRecorrido.setOnAction(e -> dibujarRecorrido(recorridos));

            cardViaje.getChildren().addAll(lblTitulo, contenedorTramos, cardResumen, btnVerRecorrido);
            contenedorRecorridos.getChildren().add(cardViaje);
        }
    }

    /**
     * Crea y configura el componente Swing que muestra el mapa de OpenStreetMap.
     * 
     * <p>Este método inicializa el {@link JXMapViewer} dentro de un {@link SwingNode},
     * configurando los eventos de movimiento y zoom.</p>
     * 
     * @param swingNode nodo JavaFX que contendrá el componente Swing.
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
     * Aumenta el nivel de zoom del mapa para acercar la vista.
     */
    @FXML
    private void handleZoomIn() {
        if (mapViewer != null) {
            int zoom = mapViewer.getZoom();
            mapViewer.setZoom(Math.max(zoom - 1, 1));
        }
    }

    /**
     * Disminuye el nivel de zoom del mapa para alejar la vista.
     */
    @FXML
    private void handleZoomOut() {
        if (mapViewer != null) {
            int zoom = mapViewer.getZoom();
            mapViewer.setZoom(Math.min(zoom + 1, 19));
        }
    }

    /**
     * Limpia todos los campos de la interfaz, incluyendo los ComboBox y los resultados mostrados.
     */
    @FXML
    private void handleLimpiarInterfaz() {
        limpiarInterfaz();
    }

    // =====================================================
    // =====================================================


    @FXML
    private void handleSalir(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    /**
     * Se llama cuando el usuario cambia la selección de idioma.
     */
    @FXML
    private void handleIdiomaChange(ActionEvent event) {

        if (menuIdiomaEs.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_ES);
            actualizarIdioma(); 
        } else if (menuIdiomaEn.isSelected()) {
            coordinador.setIdioma(Constantes.IDIOMA_EN);
            actualizarIdioma();
        }
    }

    // /**
    //  * Se llama cuando el usuario cambia la selección de tipo de archivo.
    //  */
    // @FXML
    // private void handleArchivoChange(ActionEvent event) {
    //     // Para saber cuál está seleccionado, puedes usar el ToggleGroup
    //     RadioMenuItem seleccionado = (RadioMenuItem) grupoArchivos.getSelectedToggle();
        
    //     if (seleccionado != null) {
    //         String tipoArchivo = seleccionado.getText();
    //         System.out.println("Tipo de archivo seleccionado: " + tipoArchivo);
    //     }
    // }

    // =====================================================
    // =====================================================

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
     * Verifica y normaliza los datos ingresados por el usuario.
     * 
     * <p>Controla que los campos estén completos, que las paradas de origen y destino sean distintas,
     * y que la hora ingresada sea válida.</p>
     * 
     * @return true si los datos son válidos, false si hay errores.
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

    /**
     * Muestra una alerta de error con un título y un mensaje personalizados.
     * 
     * @param titulo título de la ventana de alerta.
     * @param mensaje texto descriptivo del error.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(rb.getString(titulo));
        alert.setHeaderText(null);
        alert.setContentText(rb.getString(mensaje));
        alert.showAndWait();
    }

    /**
     * Genera un color pastel aleatorio en formato hexadecimal.
     * 
     * <p>Se usa para darle una identidad visual distinta a cada recorrido mostrado.</p>
     * 
     * @return color en formato hexadecimal (por ejemplo, "#A4D3EE").
     */
    private String generarColorAleatorio() {
        float hue = generator.nextFloat();
        float saturation = 0.25f + (generator.nextFloat() * 0.20f);
        float brightness = 0.85f + (generator.nextFloat() * 0.10f);
        java.awt.Color color = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Dibuja visualmente el recorrido seleccionado sobre el mapa.
     * 
     * <p>Convierte las paradas en coordenadas geográficas y las traza en el {@link JXMapViewer}.</p>
     * 
     * @param recorridos lista de recorridos que conforman un viaje completo.
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

    /**
     * Muestra un mensaje en la interfaz cuando no existen recorridos disponibles
     * entre las paradas seleccionadas.
     * 
     * @param contenedorDeRecorridos panel donde se insertará el mensaje.
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
    public void actualizarIdioma() {
        if (coordinador == null || coordinador.getResourceBundle() == null) {
            System.err.println("⚠️ No se pudo actualizar el idioma: coordinador o configuración nulos");
            return;
        }

        ResourceBundle bundle = coordinador.getResourceBundle();

        // Labels principales
        lblTitulo.setText(bundle.getString("app.title"));
        lblDia.setText(bundle.getString("label.day") + ":");
        lblOrigen.setText(bundle.getString("label.origin") + ":");
        lblDestino.setText(bundle.getString("label.destination") + ":");
        lblHorario.setText(bundle.getString("label.time") + ":");
        lblRutasDisponibles.setText(bundle.getString("label.available_routes"));

        // Botones
        btnMostrarRecorrido.setText(bundle.getString("button.show_routes"));
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


        mapaDias.clear(); // Limpia el mapa anterior
        mapaDias.put(bundle.getString("cbx.day.monday"), 1);
        mapaDias.put(bundle.getString("cbx.day.tuesday"), 2);
        mapaDias.put(bundle.getString("cbx.day.wednesday"), 3);
        mapaDias.put(bundle.getString("cbx.day.thursday"), 4);
        mapaDias.put(bundle.getString("cbx.day.friday"), 5);
        mapaDias.put(bundle.getString("cbx.day.saturday"), 6);
        mapaDias.put(bundle.getString("cbx.day.sunday"), 7);

        // Actualizar los días según idioma
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

        contenedorRecorridos.getChildren().clear();
        rb = coordinador.getResourceBundle();
    }

}
