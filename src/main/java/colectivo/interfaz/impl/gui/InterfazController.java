package colectivo.interfaz.impl.gui;

import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import colectivo.controlador.Coordinador;
import colectivo.coordinador.Coordinable;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Controlador de la interfaz gráfica de usuario (GUI) para la simulación de colectivos.
 * Implementa Initializable para el ciclo de vida de FXML y Coordinable para la inyección de dependencias.
 */
public class InterfazController implements Initializable, Coordinable {

    // --- Componentes FXML ---
    @FXML private AnchorPane anchorPaneFrmPrincipal;
    @FXML private ComboBox<Parada> cbxDestino;
    @FXML private ComboBox<Parada> cbxOrigen;
    @FXML private ComboBox<String> cbxDia;
    @FXML private ComboBox<String> cbxHora;
    @FXML private ComboBox<String> cbxMinuto;
    @FXML private Label lblDestino;
    @FXML private Label lblDia;
    @FXML private Label lblHorario;
    @FXML private Button btnMostrarRecorrido; // Botón de calcular
    @FXML private Label lblOrigen;
    @FXML private Label lblTitulo;
    @FXML private ScrollPane scrResultados;
    @FXML private VBox vboxResultados;
    
    // --- Atributos de Lógica ---
    
    /** Mapea el nombre del día (String) a su valor numérico (int) para el coordinador. */
    private final Map< String, Integer > mapaDias= new HashMap<>();

    private Coordinador coordinador;

    // ------------------------------------
    // MÉTODOS DE LA INTERFAZ Y CICLO DE VIDA
    // ------------------------------------

    /**
     * Inyecta la dependencia del Coordinador.
     * @param coordinador La instancia del Coordinador.
     */
    @Override
    public void setCoordinador(Coordinador coordinador) {
        this.coordinador = coordinador;
    }

    /**
     * Método de inicialización de JavaFX. Se llama automáticamente después de cargar el FXML.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializar el mapa de días (Mapeo String -> Integer)
        mapaDias.put("Lunes",1);
        mapaDias.put("Martes",2);
        mapaDias.put("Miercoles",3);
        mapaDias.put("Jueves",4);
        mapaDias.put("Viernes",5);
        mapaDias.put("Sabado",6);
        mapaDias.put("Domingo o feriado",7);
        
        // La carga de datos dependientes del coordinador se realiza externamente (cargarDatosIniciales).
    }
    
    // ------------------------------------
    // LÓGICA DE CARGA DE DATOS
    // ------------------------------------

    /**
     * Llena los ComboBox con datos obtenidos del coordinador. 
     * Este método es llamado externamente cuando el Coordinador ya ha sido inyectado.
     */
    public void cargarDatosIniciales() {

        if (coordinador == null) {
            System.err.println("Coordinador no inicializado. No se pudo cargar la información.");
            return;
        }

        // 1. Llenar cbxDia
        List<String>dias = List.of("Lunes","Martes","Miercoles","Jueves","Viernes","Sabado","Domingo o feriado");
        cbxDia.setItems(FXCollections.observableArrayList(dias));
        // if (!dias.isEmpty()) {
        //      cbxDia.getSelectionModel().select(0); // Seleccionar el primer día (Lunes)
        // }
        
        // 2. Llenar cbxHora y cbxMinuto
        List<String> horas = new ArrayList<>();
        List<String> minutos = new ArrayList<>();

        for (int h = 1; h <= 22; h++) {
            horas.add(String.format("%02d", h));
        }
        for (int m = 0; m <= 59; m++) {
            minutos.add(String.format("%02d", m));
        }

        cbxHora.setItems(FXCollections.observableArrayList(horas));
        cbxMinuto.setItems(FXCollections.observableArrayList(minutos));
        // cbxHora.getSelectionModel().select("10"); 
        // cbxMinuto.getSelectionModel().select("00"); 

        // 3. Llenar cbxOrigen y cbxDestino con objetos Parada
        List<Parada> paradas = new ArrayList<>(coordinador.listarParadas().values());

        cbxOrigen.setItems(FXCollections.observableArrayList(paradas));
        cbxDestino.setItems(FXCollections.observableArrayList(paradas));
        
        // Define el conversor para mostrar la dirección en lugar del objeto Parada
        StringConverter<Parada> paradaConverter = new StringConverter<>() {
            @Override
            public String toString(Parada parada) {
                return (parada != null) ? parada.getDireccion() : "";
            }

            @Override
            public Parada fromString(String string) {
                return null; // No es necesario para ComboBox de selección simple
            }
        };

        cbxOrigen.setConverter(paradaConverter);
        cbxDestino.setConverter(paradaConverter);

        // Seleccionar valores por defecto
        // if (!paradas.isEmpty()) {
        //     cbxOrigen.getSelectionModel().select(0);
        //     cbxDestino.getSelectionModel().select(paradas.size() > 1 ? 1 : 0); 
        // }
    }

    // ------------------------------------
    // LÓGICA DE EVENTOS Y CÁLCULO
    // ------------------------------------

    /**
     * Maneja el evento del botón "Calcular Recorrido".
     * Llama al coordinador con los parámetros seleccionados por el usuario.
     */
    @FXML
    private void handleCalcularRecorrido() {
        if (coordinador == null) {
            mostrarAlerta("Error de Configuración", "El coordinador no ha sido inicializado.");
            return;
        }
    
        // 1. Obtener valores y mapear objetos
        String diaStr = cbxDia.getValue();
        Parada origen = cbxOrigen.getValue();
        Parada destino = cbxDestino.getValue();
        String horaStr = cbxHora.getValue();     
        String minutoStr = cbxMinuto.getValue();

        // Validación simple de nulos
        if (diaStr == null || origen == null || destino == null || horaStr == null || minutoStr == null) {
            mostrarAlerta("Datos Incompletos", "Debe seleccionar todos los campos.");
            return;
        }

        try {
            // Mapeo String a tipos de datos requeridos por el coordinador
            int dia = mapaDias.get(diaStr);
            String horarioCompleto = horaStr + ":" + minutoStr;
            LocalTime hora = LocalTime.parse(horarioCompleto);

            // 2. Llamar al coordinador para calcular recorridos
            List<List<Recorrido>> recorridos = coordinador.calcularRecorrido(origen, destino, dia, hora);
            
            // 3. Mostrar resultados
            mostrarResultadosGUI(recorridos, origen, destino, hora);
                
        } catch (Exception e) {
            mostrarAlerta("Error de Cálculo", "Ocurrió un error al procesar el recorrido: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------
    // LÓGICA DE VISUALIZACIÓN DE RESULTADOS
    // ------------------------------------

    /**
     * Muestra los resultados calculados en el VBox (vboxResultados) con formato visual.
     * @param listaRecorridos La lista de opciones de viaje.
     * @param paradaOrigen Parada inicial seleccionada.
     * @param paradaDestino Parada final seleccionada.
     * @param horaLlegaPasajero Hora en la que el usuario llega a la parada de origen.
     */
    public void mostrarResultadosGUI(List<List<Recorrido>> listaRecorridos,
                            Parada paradaOrigen,
                            Parada paradaDestino,
                            LocalTime horaLlegaPasajero) {
    
        vboxResultados.getChildren().clear(); // Limpia resultados anteriores

        if (listaRecorridos == null || listaRecorridos.isEmpty()) {
            vboxResultados.getChildren().add(new Label("❌ No hay recorridos recomendados con esos criterios."));
            return;
        }

        for (int i = 0; i < listaRecorridos.size(); i++) {
            List<Recorrido> recorridos = listaRecorridos.get(i);
            VBox rutaBox = new VBox(5);
            
            // Estilo de la caja principal de la opción (Ocupa el máximo ancho)
            rutaBox.setStyle("-fx-border-color: #2196F3; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #E3F2FD;");
            rutaBox.setMaxWidth(Double.MAX_VALUE);
            
            Label lblOpcion = new Label("OPCIÓN DE VIAJE #" + (i + 1));
            lblOpcion.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2; -fx-font-size: 14px;");
            rutaBox.getChildren().add(lblOpcion);
            rutaBox.getChildren().add(new Label("------------------ TRAMOS ------------------"));
            
            // Detalles de cada tramo/recorrido
            for (Recorrido r : recorridos) {
                
                Label lblLinea = new Label("🚌 Línea: " + r.getLinea().getNombre());
                lblLinea.setStyle("-fx-font-weight: bold;");
                rutaBox.getChildren().add(lblLinea);
                
                rutaBox.getChildren().add(new Label("  Origen: " + r.getParadas().getFirst().getDireccion()));
                rutaBox.getChildren().add(new Label("  Salida estimada: " + r.getHoraSalida()));
                rutaBox.getChildren().add(new Label("  Destino: " + r.getParadas().getLast().getDireccion()));
                rutaBox.getChildren().add(new Label("  Duración del tramo: " + Tiempo.segundosATiempo(r.getDuracion())));
                
                // Muestra un separador si este no es el último tramo (indica un transbordo)
                if (r != recorridos.getLast()) {
                    rutaBox.getChildren().add(new Label("--- 🔄 TRANSBORDO ---"));
                }
            }
            
            // RESUMEN FINAL
            Recorrido ultimoRecorrido = recorridos.getLast();
            
            // Cálculo de resumen
            LocalTime duracionTotal = Tiempo.calcularDuracionTotalViaje(ultimoRecorrido, horaLlegaPasajero);
            LocalTime horaLlegada = Tiempo.calcularHoraLlegadaDestino(ultimoRecorrido);
            
            rutaBox.getChildren().add(new Label("================================="));
            
            Label lblDuracionTotal = new Label("⏳ Duración total del viaje: " + duracionTotal);
            Label lblLlegada = new Label("🏁 Hora de llegada estimada: " + horaLlegada);
            
            lblLlegada.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #388E3C;");
            
            rutaBox.getChildren().addAll(lblDuracionTotal, lblLlegada);
            
            vboxResultados.getChildren().add(rutaBox);
        }
    }
    
    // ------------------------------------
    // MÉTODOS DE UTILIDAD
    // ------------------------------------
    
    /**
     * Muestra una ventana de alerta de error.
     * @param titulo Título de la alerta.
     * @param mensaje Contenido del mensaje.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}