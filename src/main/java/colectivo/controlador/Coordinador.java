package colectivo.controlador;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;
import colectivo.negocio.Calculo;
import colectivo.negocio.SistemaColectivo;
import colectivo.servicio.InterfazService;

/**
 * Coordinador actúa como intermediario entre la interfaz de usuario y la lógica del sistema de colectivos.
 * Permite acceder a las entidades del sistema como líneas, paradas y tramos, así como a los cálculos asociados.
 */
public class Coordinador {

    /** Instancia del sistema de colectivos que mantiene todas las entidades. */
    private SistemaColectivo sistema;

    /** Instancia para realizar cálculos sobre el sistema (tiempos, recorridos, etc.). */
    private Calculo calculo;

    /** Interfaz de usuario asociada al coordinador. */
    private InterfazService interfaz;
    
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
    public List<List<Recorrido>> calcularRecorrido(Parada origen, Parada destino, int dia, LocalTime hora) {
        // Aquí delega al servicio de cálculo
        return calculo.calcularRecorrido(origen, destino, dia, hora, sistema.getTramos());
    }
    public void iniciar(){
        interfaz.iniciar();
    }
}
