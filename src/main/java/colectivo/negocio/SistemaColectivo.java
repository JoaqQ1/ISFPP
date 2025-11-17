package colectivo.negocio;

import java.util.Map;

import colectivo.controlador.Coordinable;
import colectivo.controlador.CoordinadorApp;
import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.servicio.LineaService;
import colectivo.servicio.ParadaService;
import colectivo.servicio.TramoService;

/**
 * Clase que representa el sistema de colectivos.
 * Implementa el patrón Singleton para garantizar una única instancia
 * y centralizar el acceso a las colecciones de líneas, paradas y tramos.
 */
public class SistemaColectivo implements Coordinable{

    /** Instancia única del sistema (Singleton). */
    private static SistemaColectivo instancia = null;

    /** Colecciones de datos cargados desde los servicios. */
    private Map<String, Linea> lineas;
    private Map<Integer, Parada> paradas;
    private Map<String, Tramo> tramos;

    private CoordinadorApp coordinador;

    /**
     * Constructor privado que inicializa los servicios y carga los datos.
     * Se utiliza únicamente dentro del método {@link #getInstancia()}.
     */
    public SistemaColectivo(Map<String, Linea> lineas, Map<Integer, Parada> paradas, Map<String, Tramo> tramos) {
        this.lineas = lineas;
        this.paradas = paradas;
        this.tramos = tramos;
        
        instancia = this;
    }

    /**
     * Obtiene la instancia única del sistema.
     * Si aún no existe, la crea.
     *
     * @return instancia única de {@link SistemaColectivo}
     */
    public static SistemaColectivo getInstancia() {
        return instancia;
    }

    /**
     * Devuelve todas las líneas del sistema.
     *
     * @return mapa de líneas indexadas por su código
     */
    public Map<String, Linea> getLineas() {
        return lineas;
    }

    /**
     * Devuelve todas las paradas del sistema.
     *
     * @return mapa de paradas indexadas por su código
     */
    public Map<Integer, Parada> getParadas() {
        return paradas;
    }

    /**
     * Devuelve todos los tramos del sistema.
     *
     * @return mapa de tramos indexados por un identificador único
     */
    public Map<String, Tramo> getTramos() {
        return tramos;
    }

    public void setCoordinador(CoordinadorApp coordinador){
        this.coordinador = coordinador;
    }
}
