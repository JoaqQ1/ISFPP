package colectivo.controlador;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.constantes.Constantes;
import colectivo.excepciones.AppException;
import colectivo.excepciones.ConfiguracionException;
import colectivo.excepciones.FactoryException;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import colectivo.negocio.Calculo;
import colectivo.negocio.SistemaColectivo;
import colectivo.servicio.InterfazService;
import colectivo.servicio.InterfazServiceImpl;
import colectivo.servicio.LineaService;
import colectivo.servicio.LineaServiceImpl;
import colectivo.servicio.ParadaService;
import colectivo.servicio.ParadaServiceImpl;
import colectivo.servicio.TramoService;
import colectivo.servicio.TramoServiceImpl;

/**
 * Coordinador actúa como intermediario entre la interfaz de usuario y la lógica del sistema de colectivos.
 * Permite acceder a las entidades del sistema como líneas, paradas y tramos, así como a los cálculos asociados.
 */
public class CoordinadorApp implements ICoordinador{

    private static final Logger LOGGER = LogManager.getLogger(CoordinadorApp.class.getName());

    /** Instancia del sistema de colectivos que mantiene todas las entidades. */
    private SistemaColectivo sistema;

    /** Instancia para realizar cálculos sobre el sistema (tiempos, recorridos, etc.). */
    private Calculo calculo;

    /** Interfaz de usuario asociada al coordinador. */
    private InterfazService interfaz;

    private ConfiguracionGlobal config;

    private Map<String, Object> datos;
    
    /** Servicios para acceder a los datos de líneas, paradas y tramos. */
    private LineaService lineaService;
    private ParadaService paradaService;
    private TramoService tramoService;
    
    /** Colecciones de datos cargados desde los servicios. */
    private Map<String, Linea> lineas;
    private Map<Integer, Parada> paradas;
    private Map<String, Tramo> tramos;
    /**
     * Obtiene la instancia del sistema de colectivos.
     * @return el SistemaColectivo asociado
     */
    public SistemaColectivo getSistema() {
        return sistema;
    }

    /**
     * Asocia un sistema de colectivos al coordinador.
     * @param sistema el SistemaColectivo a asociar
     */
    public void setSistema(SistemaColectivo sistema) {
        if(sistema == null) {
            LOGGER.error("setSistema: El sistema no puede ser nulo");
            throw new AppException("El sistema no puede ser nulo");
        }
        this.sistema = sistema;
        this.sistema.setCoordinador(this);
    }

    /**
     * Obtiene la instancia de cálculos asociada al coordinador.
     * @return el objeto Calculo
     */
    public Calculo getCalculo() {
        return calculo;
    }

    /**
     * Asocia un objeto de cálculo al coordinador.
     * @param calculo el objeto Calculo a asociar
     */
    public void setCalculo(Calculo calculo) {
        if(calculo == null) {
            LOGGER.error("setCalculo: El objeto de cálculo no puede ser nulo");
            throw new AppException("El objeto de cálculo no puede ser nulo");
        }
        this.calculo = calculo;
    }

    /**
     * Obtiene la interfaz de usuario asociada al coordinador.
     * @return la interfaz asociada
     */
    public InterfazService getInterfaz() {
        return interfaz;
    }

    /**
     * Asocia una interfaz de usuario al coordinador.
     * @param interfaz la interfaz a asociar
     */
    public void setInterfaz(InterfazService interfaz) {
        if(interfaz == null) {
            LOGGER.error("setInterfaz: La interfaz no puede ser nula");
            throw new AppException("La interfaz no puede ser nula");
        }
        this.interfaz = interfaz;
        this.interfaz.setCoordinador(this);
    }

    /**
     * Devuelve todas las líneas de colectivo del sistema.
     * @return un mapa con las líneas indexadas por su código
     */
    public Map<String, Linea> listarLineas() {
        return sistema.getLineas();
    }

    /**
     * Devuelve todos los tramos del sistema.
     * @return un mapa con los tramos indexados por su identificador
     */
    public Map<String, Tramo> listarTramos() {
        return sistema.getTramos();
    }

    /**
     * Devuelve todas las paradas del sistema.
     * @return un mapa con las paradas indexadas por su código
     */
    public Map<Integer, Parada> listarParadas() {
        return sistema.getParadas();
    }
    
    @SuppressWarnings("unchecked")
    public List<List<Recorrido>> calcularRecorrido(Parada origen, Parada destino, int dia, LocalTime hora) {
        if(origen == null || destino == null) {
            LOGGER.error("calcularRecorrido: Parada de origen o destino es nula");
            throw new AppException("Parada de origen y destino no pueden ser nulas");
        }
        if(dia < 0 || dia > 6) {
            LOGGER.error("calcularRecorrido: Día inválido proporcionado: " + dia);
            throw new AppException("Día debe estar entre 0 (Domingo) y 6 (Sábado)");
        }
        if(hora == null) {
            LOGGER.error("calcularRecorrido: Hora proporcionada es nula");
            throw new AppException("Hora no puede ser nula");
        }
        // Aquí delega al servicio de cálculo
        return calculo.calcularRecorrido(origen, destino, dia, hora, (Map<String,Tramo>)datos.get(Constantes.TRAMO));
    }
    public void iniciar(){
        interfaz.iniciar();
    }

    public void inicializarAplicacion(){
        try{
            config = ConfiguracionGlobal.getConfiguracionGlobal();
        }
        catch(ConfiguracionException e){
            
            LOGGER.error("Fallo al inciar la app: ",e.getMessage());
            throw new AppException("No se pudo obtener la configuracion.",e);
        }

        inicializarServicios();
        LOGGER.info("Se cargo la interfaz correctamente");
        cargarDatos();
        LOGGER.info("Se cargaron los datos correctamente");
        inicializarNegocio();

        inicializarInterfazUsuario();
    }

    private void inicializarServicios(){
        try{
            lineaService = new LineaServiceImpl();
            paradaService = new ParadaServiceImpl();
            tramoService = new TramoServiceImpl();
            lineas = lineaService.buscarTodos();
            paradas = paradaService.buscarTodos();
            tramos = tramoService.buscarTodos();
            LOGGER.info("Servicios inciados corectamente");

        } catch(FactoryException e){
            LOGGER.fatal("Error crítico de al incializa los servicios de lectura de datos. La aplicación no puede iniciar.", e);
            throw new AppException("Error critico al inicializar los servicios de lectura de datos.",e);
        } catch(ConfiguracionException e){
            String errorMsg = "Error crítico al cargar los datos ";
            LOGGER.fatal(errorMsg + " Causa original: " + e.getMessage(), e);
            throw new AppException(errorMsg + " Detalles: " + e.getMessage(), e);
        }

		sistema = new SistemaColectivo(lineas, paradas, tramos);
		sistema.setCoordinador(this);
    }

    private void cargarDatos(){
        datos = new HashMap<>();
        datos.put(Constantes.PARADA,sistema.getParadas());
        datos.put(Constantes.LINEA,sistema.getLineas());
        datos.put(Constantes.TRAMO,sistema.getTramos());
    }
    private void inicializarNegocio(){
        calculo = new Calculo(datos);
    }
    private void inicializarInterfazUsuario(){
        try{
            interfaz = new InterfazServiceImpl();
            LOGGER.info("Interfaz creada correctamente");
        }catch(FactoryException e){
            String mensajeError = "Error crítico al inicializar los servicios de interfaz. La aplicación no puede iniciar.";
            LOGGER.fatal(mensajeError, e);
            throw new AppException(mensajeError, e);
        }
        interfaz.setCoordinador(this);
        interfaz.iniciar();
    }

    public ResourceBundle getResourceBundle(){
        return config.getResourceBundle();
    }
    public void setIdioma(Locale locale) {
        if(locale == null ) throw new AppException("El idioma no puede ser nulo"+locale);
        config.setLocale(locale);
    }
    
    public String getIdiomaActual() {
        return config.getIdiomaActual();
    }
    public double getOrigenLatitud() {
        return config.getOrigenLatitud();
    }
    public double getOrigenLongitud() {
        return config.getOrigenLongitud();
    }
    public int getZoom() {
        return config.getZoom();
    } 
}
