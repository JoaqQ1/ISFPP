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

import colectivo.controlador.Coordinador;
import colectivo.coordinador.Coordinable;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
 * con el objeto {@link Coordinador}, que maneja la lógica del dominio.</p>
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
    @FXML private ComboBox<String> cbxDia;
    @FXML private ComboBox<Parada> cbxOrigen;
    @FXML private ComboBox<Parada> cbxDestino;
    @FXML private ComboBox<String> cbxHora;
    @FXML private ComboBox<String> cbxMinuto;
    @FXML private Button btnMostrarRecorrido; 
    @FXML private Button btnLimpiarInterfaz;
    @FXML private Button btnZoomIn, btnZoomOut;
    @FXML private SwingNode swingNodeMapa;

    private JXMapViewer mapViewer;
    private final Map<String, Integer> mapaDias = new HashMap<>();
    private Coordinador coordinador;
    private static Random generator = new Random();

    /**
     * Inyecta la referencia del {@link Coordinador} en este controlador.
     * 
     * <p>Esto permite que la interfaz gráfica acceda a los métodos de negocio y datos
     * del sistema (por ejemplo, listar paradas o calcular recorridos).</p>
     * 
     * @param coordinador instancia principal de Coordinador que maneja la lógica de la aplicación.
     */
    @Override
    public void setCoordinador(Coordinador coordinador) {
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

        cbxDia.setPromptText("Seleccione el día");
        cbxOrigen.setPromptText("Seleccione la parada de origen");
        cbxDestino.setPromptText("Seleccione la parada de destino");
        cbxHora.setPromptText("Seleccione la hora");
        cbxMinuto.setPromptText("Seleccione el minuto");
        
        mapaDias.put("Lunes",1);
        mapaDias.put("Martes",2);
        mapaDias.put("Miercoles",3);
        mapaDias.put("Jueves",4);
        mapaDias.put("Viernes",5);
        mapaDias.put("Sabado",6);
        mapaDias.put("Domingo o feriado",7);

        // Llenado de los ComboBox
        List<String> dias = List.of("Lunes","Martes","Miercoles","Jueves","Viernes","Sabado","Domingo o feriado");
        cbxDia.setItems(FXCollections.observableArrayList(dias));

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
            mostrarAlerta("Error de Cálculo", "Ocurrió un error al procesar el recorrido: " + e.getMessage());
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
            Label lblTitulo = new Label("Recorrido sugerido");
            lblTitulo.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #222;");

            VBox contenedorTramos = new VBox(8);
            contenedorTramos.setFillWidth(true);

            // Para cada tramo del viaje creamos una tarjeta con sus datos
            for (Recorrido r : recorridos) {
                String linea = (r.getLinea() != null) ? "Línea: " + r.getLinea().getNombre() : "Caminando";

                // Construimos texto de paradas separadas por flecha
                List<Parada> paradasList = r.getParadas();
                String paradasTexto = paradasList.stream()
                        .map(Parada::getDireccion)
                        .collect(Collectors.joining(" → "));

                String paradas = "Paradas: " + paradasTexto;
                String hora = "Hora de salida: " + r.getHoraSalida();
                String duracion = "Duración: " + Tiempo.segundosATiempo(r.getDuracion());

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
            String duracionTotal = "Duración total: " + Tiempo.calcularDuracionTotalViaje(ultimo, horaLlegaParada);
            String horaLlegada = "Hora de llegada: " + Tiempo.calcularHoraLlegadaDestino(ultimo);

            Label lblDuracionTotal = new Label(duracionTotal);
            lblDuracionTotal.setStyle("-fx-text-fill: #222; -fx-font-weight: bold; -fx-font-size: 14;");

            Label lblHoraLlegada = new Label(horaLlegada);
            lblHoraLlegada.setStyle("-fx-text-fill: #333; -fx-font-size: 13;");

            cardResumen.getChildren().addAll(lblDuracionTotal, lblHoraLlegada);

            // Botón que permite ver el recorrido en el mapa (delegando a dibujarRecorrido)
            Button btnVerRecorrido = new Button("Ver recorrido");
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
            mostrarAlerta("Datos Incompletos", "Debe completar todos los campos antes de continuar.");
            return false;
        }
        if (origen.equals(destino)) {
            mostrarAlerta("Datos Inválidos", "La parada de origen y destino no pueden ser iguales.");
            return false;
        }
        try {
            int hora = Integer.parseInt(horaStr);
            int minuto = Integer.parseInt(minutoStr);
            if (hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
                mostrarAlerta("Hora Inválida", "Ingrese una hora válida entre 00:00 y 23:59.");
                return false;
            }
            cbxHora.setValue(String.format("%02d", hora));
            cbxMinuto.setValue(String.format("%02d", minuto));
        } catch (NumberFormatException e) {
            mostrarAlerta("Formato Inválido", "La hora y los minutos deben ser números válidos.");
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
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
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
        VBox cardSinResultados = new VBox();
        cardSinResultados.setPadding(new Insets(40));
        cardSinResultados.setAlignment(Pos.CENTER);
        cardSinResultados.setStyle("""
            -fx-background-color: #fdfdfd;
            -fx-background-radius: 14;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3);
        """);

        Label lblMensaje = new Label("No hay recorridos recomendados");
        lblMensaje.setStyle("""
            -fx-font-size: 18;
            -fx-font-family: Helvetica;
            -fx-text-fill: #666;
            -fx-font-weight: bold;
        """);

        Label lblSugerencia = new Label("Por favor, pruebe cambiando la hora o las paradas seleccionadas.");
        lblSugerencia.setStyle("""
            -fx-font-size: 14;
            -fx-font-family: Helvetica;
            -fx-text-fill: #999;
        """);

        cardSinResultados.getChildren().addAll(lblMensaje, lblSugerencia);
        contenedorDeRecorridos.getChildren().add(cardSinResultados);
    }
}
