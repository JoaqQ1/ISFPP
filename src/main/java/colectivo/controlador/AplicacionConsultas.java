package colectivo.controlador;

import java.io.IOException;

import colectivo.negocio.Calculo;
import colectivo.negocio.SistemaColectivo;
import colectivo.servicio.InterfazService;
import colectivo.servicio.InterfazServiceImpl;

/**
 * Clase principal de la aplicación de consultas del sistema de colectivos.
 * Permite al usuario calcular y mostrar recorridos entre paradas según día y hora.
 */
public class AplicacionConsultas {

    /** Objeto para realizar cálculos de recorridos, tiempos, etc. */
    private Calculo calculo;

    /** Interfaz de usuario para ingresar datos y mostrar resultados. */
    private InterfazService interfaz;

    /** Coordinador que actúa como intermediario entre la interfaz y el sistema. */
    private Coordinador coordinador;

    SistemaColectivo sistemaColectivo;

    /**
     * Método principal que inicia la aplicación.
     * @param args argumentos de línea de comando (no utilizados)
     * @throws IOException si ocurre un error al cargar los datos del sistema
     */
    public static void main(String[] args) throws IOException {
        AplicacionConsultas miAplicacion = new AplicacionConsultas();
        miAplicacion.inciar();
    }

    /**
     * Inicializa los componentes principales de la aplicación:
     * el coordinador, los cálculos y la interfaz de usuario.
     */
    private void inciar() {
        
        coordinador = new Coordinador();
        calculo = new Calculo();
        interfaz = new InterfazServiceImpl();
        sistemaColectivo = SistemaColectivo.getInstancia();

        coordinador.setCalculo(calculo);
        coordinador.setInterfaz(interfaz);
        coordinador.setSistema(sistemaColectivo);
        
        coordinador.iniciar();
    }
}
