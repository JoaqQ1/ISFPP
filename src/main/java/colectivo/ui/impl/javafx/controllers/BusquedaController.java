package colectivo.ui.impl.javafx.controllers;

import colectivo.controlador.ICoordinador;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.AsyncService;
import colectivo.util.Tiempo;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Micro-Controlador: Lógica de Búsqueda, Validación y Resultados.
 * Responsabilidad: "Mesero" que toma el pedido, lo manda a cocina (Async) 
 * y sirve los resultados visuales.
 */
public class BusquedaController {

    private static final Logger LOGGER = LogManager.getLogger(BusquedaController.class);
    private static final Random generator = new Random();

    // --- 1. Referencias a Componentes UI (Inyectados desde InterfazController) ---
    private ComboBox<Parada> cbxOrigen;
    private ComboBox<Parada> cbxDestino;
    private ComboBox<String> cbxDia;
    private Spinner<Integer> spinnerHora;
    private Spinner<Integer> spinnerMinuto;
    private Button btnCalcular;
    private Button btnCancelar;
    private Button btnLimpiar;
    private ProgressIndicator spinnerLoading;
    private VBox contenedorRecorridos;

    private ICoordinador coordinador;
    private AsyncService asyncService;
    private MapaController mapaController; 
    private ResourceBundle rb;
    private final Map<String, Integer> mapaDias = new HashMap<>();
    private List<String> listaDiasUI = new ArrayList<>(); // Para el combo

    /**
     * Recibe las dependencias externas necesarias.
     */
    public void configurarDependencias(ICoordinador coord, AsyncService async, MapaController mapa) {
        this.coordinador = coord;
        this.asyncService = async;
        this.mapaController = mapa;
        this.rb = coord.getResourceBundle(); // Obtener el RB actual
    }

    /**
     * Recibe los controles visuales del FXML principal.
     */
    public void setControlesVisuales(
            ComboBox<Parada> cbxOrigen, ComboBox<Parada> cbxDestino, ComboBox<String> cbxDia,
            Spinner<Integer> spinnerHora, Spinner<Integer> spinnerMinuto,
            Button btnCalcular, Button btnCancelar, Button btnLimpiar,
            ProgressIndicator spinnerLoading, VBox contenedorRecorridos) {
        
        this.cbxOrigen = cbxOrigen;
        this.cbxDestino = cbxDestino;
        this.cbxDia = cbxDia;
        this.spinnerHora = spinnerHora;
        this.spinnerMinuto = spinnerMinuto;
        this.btnCalcular = btnCalcular;
        this.btnCancelar = btnCancelar;
        this.btnLimpiar = btnLimpiar;
        this.spinnerLoading = spinnerLoading;
        this.contenedorRecorridos = contenedorRecorridos;
    }

    /**
     * Carga los datos en los combos y configura el autocompletado.
     * Debe llamarse después de configurarDependencias y setControlesVisuales.
     */
    public void inicializarDatosUI() {
        actualizarTextosEIdioma(); // Carga textos y días
        cargarParadas();           // Carga paradas y configura autocomplete
    }

    private void cargarParadas() {
        // Usamos el coordinador para obtener datos
        List<Parada> paradas = List.copyOf(coordinador.listarParadas().values());
        
        // Configuramos Autocomplete y Converter para Origen y Destino
        configurarComboParada(cbxOrigen, paradas);
        configurarComboParada(cbxDestino, paradas);
    }

    // --- 4. Lógica Principal: CALCULAR ---

    public void onCalcularRecorrido() {
        if (!validarEntrada()) return;

        // 1. Capturar datos (UI Thread)
        final Parada origen = cbxOrigen.getValue();
        final Parada destino = cbxDestino.getValue();
        final int dia = mapaDias.get(cbxDia.getValue());
        final LocalTime hora = LocalTime.of(spinnerHora.getValue(), spinnerMinuto.getValue());

        // 2. Preparar UI (Estado "Cargando")
        setModoCargando(true);
        contenedorRecorridos.getChildren().clear();
        mapaController.limpiarMapa(); // Limpiamos el mapa antes de calcular

        // 3. Ejecutar en Background (AsyncService)
        asyncService.ejecutarAsync(
            // TAREA (Cocinero)
            () -> {
                LOGGER.info("Iniciando cálculo de ruta en background...");
                // Simulamos delay si quieres probar el spinner (opcional)
                try { Thread.sleep(2000); } catch (InterruptedException e) {} 
                return coordinador.calcularRecorrido(origen, destino, dia, hora);
            },
            
            // ÉXITO (Mesero - UI Thread)
            resultados -> {
                setModoCargando(false);
                mostrarRecorridos(resultados, hora);
                LOGGER.info("Cálculo finalizado. Resultados: " + (resultados != null ? resultados.size() : 0));
            },
            
            // ERROR (Mesero - UI Thread)
            excepcion -> {
                setModoCargando(false);
                LOGGER.error("onCalcularRecorrido:Error calculando ruta", excepcion);
                mostrarAlerta("alert.title.calc_error", "alert.calc_error_body");
            }
        );
    }

    public void onLimpiarInterfaz() {
        cbxOrigen.setValue(null);
        cbxDestino.setValue(null);
        cbxDia.setValue(null);
        contenedorRecorridos.getChildren().clear();
        mapaController.limpiarMapa();
        spinnerHora.getValueFactory().setValue(12);
        spinnerMinuto.getValueFactory().setValue(0);
    }

    // --- 5. Métodos de UI Dinámica (Cards) ---

    private void mostrarRecorridos(List<List<Recorrido>> listaDeRecorridos, LocalTime horaLlegada) {
        if (listaDeRecorridos == null || listaDeRecorridos.isEmpty()) {
            mostrarCardSinResultados();
            return;
        }

        for (List<Recorrido> viaje : listaDeRecorridos) {
            // Creamos la tarjeta visual (Tu lógica original, simplificada aquí)
            VBox card = crearTarjetaViaje(viaje, horaLlegada);
            contenedorRecorridos.getChildren().add(card);
        }
    }

    private VBox crearTarjetaViaje(List<Recorrido> recorridos, LocalTime horaLlegaParada) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #fdfdfd; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 8, 0, 0, 3);");

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
        
        // Botón "Ver en Mapa"
        Button btnVerMapa = new Button(rb.getString("button.view_route")); // "Ver en Mapa"
        btnVerMapa.setStyle("-fx-background-color: #4ECDC4; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        
        // ACCIÓN CLAVE: Conectar con el MapaController
        btnVerMapa.setOnAction(e -> {
            LOGGER.info("Enviando recorrido al MapaController");
            mapaController.dibujarRecorrido(recorridos);
        });

        card.getChildren().addAll(lblTituloCard, contenedorTramos, cardResumen, btnVerMapa);
        return card;
    }

    private void setModoCargando(boolean cargando) {
        spinnerLoading.setVisible(cargando);
        contenedorRecorridos.setVisible(!cargando);
        
        btnCalcular.setVisible(!cargando);
        btnCancelar.setVisible(cargando); // Botón cancelar aparece solo cargando
        
        btnLimpiar.setDisable(cargando);
        cbxOrigen.setDisable(cargando);
        cbxDestino.setDisable(cargando);
        cbxDia.setDisable(cargando);
    }

    // --- 6. Helpers de Validación y Configuración ---

    private boolean validarEntrada() {
        if (cbxOrigen.getValue() == null || cbxDestino.getValue() == null || cbxDia.getValue() == null) {
            mostrarAlerta("alert.title.incomplete", "alert.incomplete");
            return false;
        }
        if (cbxOrigen.getValue().equals(cbxDestino.getValue())) {
            mostrarAlerta("alert.title.invalid_data", "alert.invalid_stops");
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String keyTitulo, String keyMensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(rb.containsKey(keyTitulo) ? rb.getString(keyTitulo) : keyTitulo);
        alert.setContentText(rb.containsKey(keyMensaje) ? rb.getString(keyMensaje) : keyMensaje);
        alert.show();
    }

    private void mostrarCardSinResultados() {
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
        contenedorRecorridos.getChildren().add(cardSinResultados);
        LOGGER.info("Se mostró el mensaje de 'sin resultados' en la interfaz.");
    }

    /**
     * Configura el autocompletado y converter para un combo de paradas.
     */
    private void configurarComboParada(ComboBox<Parada> combo, List<Parada> paradas) {
        // Converter para mostrar solo la dirección
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Parada p) { return (p != null) ? p.getDireccion() : ""; }
            @Override
            public Parada fromString(String s) {
                return paradas.stream()
                        .filter(p -> p.getDireccion().equalsIgnoreCase(s))
                        .findFirst().orElse(null);
            }
        });
        
        // Reutilizamos tu lógica de autocompletado genérica
        setupAutoComplete(combo, paradas, Parada::getDireccion);
    }
    
    // Tu método setupAutoComplete original (privado dentro de este controlador)
    private <T> void setupAutoComplete(ComboBox<T> comboBox, List<T> masterList, Function<T, String> stringExtractor) {
        if(comboBox == null){
            LOGGER.error("setupAutoComplete: comboBox es nulo.");
            return;
        }
        if(masterList == null){
            LOGGER.error("setupAutoComplete: masterList es nulo.");
            return;
        }
        if(masterList.isEmpty()){
            LOGGER.warn("setupAutoComplete: masterList está vacía.");
            return;
        }
        if(stringExtractor == null){
            LOGGER.error("setupAutoComplete: stringExtractor es nulo.");
            return;
        }

        // 1. Establece la lista inicial
        comboBox.setItems(FXCollections.observableArrayList(masterList));
        
        // 2. Listener para filtrar la lista mientras el usuario escribe
        comboBox.getEditor().setOnKeyReleased(event -> {
            String userInput = comboBox.getEditor().getText();

            String regexPattern = "^" + Pattern.quote(userInput) + ".*";

            
            if (userInput == null || userInput.isEmpty()) {
                comboBox.setItems(FXCollections.observableArrayList(masterList));
                comboBox.hide(); 
            } else {
                Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
                // Filtra la lista maestra usando el extractor genérico
                List<T> filteredList = masterList.stream()
                        .filter(item -> 
                            // Aquí usamos la función que pasamos como parámetro
                            pattern.matcher(stringExtractor.apply(item).toLowerCase()).find()
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

    }

    /**
     * Actualiza textos (Idioma), RELLENA los días y PRESERVA la selección.
     */
    public void actualizarTextosEIdioma() {
        // 0. Guardar el estado actual (índice seleccionado)
        int indiceSeleccionado = cbxDia.getSelectionModel().getSelectedIndex();
        
        this.rb = coordinador.getResourceBundle();
        
        // 1. Actualizar Prompts (Textos de ayuda en los inputs vacíos)
        cbxDia.setPromptText(rb.getString("label.day"));
        cbxOrigen.setPromptText(rb.getString("label.origin"));
        cbxDestino.setPromptText(rb.getString("label.destination"));

        // 2. Limpiamos mapas y listas antiguas
        mapaDias.clear();
        listaDiasUI.clear();

        // 3. Definimos la estructura de datos (Orden Lunes -> Domingo)
        agregarDia("cbx.day.monday", 1);
        agregarDia("cbx.day.tuesday", 2);
        agregarDia("cbx.day.wednesday", 3);
        agregarDia("cbx.day.thursday", 4);
        agregarDia("cbx.day.friday", 5);
        agregarDia("cbx.day.saturday", 6);
        agregarDia("cbx.day.sunday", 7);

        // 4. Asignamos la nueva lista traducida
        cbxDia.setItems(FXCollections.observableArrayList(listaDiasUI));
        
        // 5. RESTAURAR la selección
        // Si había algo seleccionado (índice >= 0), lo volvemos a seleccionar.
        // Como el orden de los días (Lun-Dom) no cambia, el índice sirve perfectamente.
        if (indiceSeleccionado >= 0 && indiceSeleccionado < listaDiasUI.size()) {
            cbxDia.getSelectionModel().select(indiceSeleccionado);
        }
        
        // 6. Actualizar textos de botones
        if(btnCalcular != null) btnCalcular.setText(rb.getString("button.show_routes"));
        if(btnCancelar != null) btnCancelar.setText(rb.getString("button.cancel"));
        if(btnLimpiar != null) btnLimpiar.setText(rb.getString("button.clear"));
        
    
    }
    /**
     * Helper para colores aleatorios (Necesario para el estilo de las cards).
     */
    private String generarColorAleatorio() {
        float hue = generator.nextFloat();
        float saturation = 0.25f + (generator.nextFloat() * 0.20f);
        float brightness = 0.85f + (generator.nextFloat() * 0.10f);
        java.awt.Color color = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
    private void agregarDia(String keyBundle, int valorDia) {
        if (rb.containsKey(keyBundle)) {
            String nombreDia = rb.getString(keyBundle);
            mapaDias.put(nombreDia, valorDia); // Para la lógica (buscar ruta)
            listaDiasUI.add(nombreDia);        // Para la UI (mostrar en orden)
        }
    }
}