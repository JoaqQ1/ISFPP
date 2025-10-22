package colectivo.controlador;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

import colectivo.interfaz.Interfaz;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.negocio.Calculo;
import colectivo.negocio.SistemaColectivo;

/**
 * Clase principal de la aplicación de consultas del sistema de colectivos.
 * Permite al usuario calcular y mostrar recorridos entre paradas según día y hora.
 */
public class AplicacionConsultas {

    /** Objeto para realizar cálculos de recorridos, tiempos, etc. */
    private Calculo calculo;

    /** Interfaz de usuario para ingresar datos y mostrar resultados. */
    private Interfaz interfaz;

    /** Coordinador que actúa como intermediario entre la interfaz y el sistema. */
    private Coordinador coordinador;

    /**
     * Método principal que inicia la aplicación.
     * @param args argumentos de línea de comando (no utilizados)
     * @throws IOException si ocurre un error al cargar los datos del sistema
     */
    public static void main(String[] args) throws IOException {
        AplicacionConsultas miAplicacion = new AplicacionConsultas();
        miAplicacion.inciar();
        miAplicacion.consultar();
    }

    /**
     * Inicializa los componentes principales de la aplicación:
     * el coordinador, los cálculos y la interfaz de usuario.
     */
    private void inciar() {
        coordinador = new Coordinador();
        calculo = new Calculo();
        interfaz = new Interfaz();

        coordinador.setCalculo(calculo);
        coordinador.setInterfaz(interfaz);
        coordinador.setSistema(SistemaColectivo.getInstancia());
    }

    /**
     * Ejecuta la consulta de recorridos entre dos paradas:
     * <ul>
     *     <li>Solicita los datos al usuario mediante la interfaz.</li>
     *     <li>Calcula los recorridos posibles.</li>
     *     <li>Muestra los resultados al usuario.</li>
     * </ul>
     */
    private void consultar() {
        // Habilita modo debug para mostrar información de ingreso
        Interfaz.setDebug(true);

        // Solicitar datos al usuario
        Parada paradaOrigen = interfaz.ingresarParadaOrigen(coordinador.listarParadas());
        Parada paradaDestino = interfaz.ingresarParadaDestino(coordinador.listarParadas());
        int diaSemana = interfaz.ingresarDiaSemana();
        LocalTime horaLlegaParada = interfaz.ingresarHoraLlegaParada();

        // Realizar cálculo de recorridos
        List<List<Recorrido>> recorridos = calculo.calcularRecorrido(
            paradaOrigen,
            paradaDestino,
            diaSemana,
            horaLlegaParada,
            coordinador.listarTramos()
        );

        // Mostrar resultado
        interfaz.resultado(recorridos, paradaOrigen, paradaDestino, horaLlegaParada);
    }
}
