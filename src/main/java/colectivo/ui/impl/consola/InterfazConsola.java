package colectivo.ui.impl.consola;

import java.time.LocalTime;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import colectivo.controlador.CoordinadorApp;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.ui.Interfaz;
import colectivo.util.Tiempo;

public class InterfazConsola implements Interfaz {
    
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Scanner sc = new Scanner(System.in);

    private CoordinadorApp coordinador;

    private static boolean debug = false;

    private static int origen = 83;
    private static int destino = 123;

    public InterfazConsola(){
        // debug = true;
    }

    public void iniciar(){
        if(coordinador == null){
            LOGGER.error("iniciar: Debe setear un coordinador primero");
            return;
        }
        Parada origen = ingresarParadaOrigen(coordinador.listarParadas());
        Parada destino = ingresarParadaDestino(coordinador.listarParadas());
        int dia = ingresarDiaSemana();
        LocalTime hora = ingresarHoraLlegaParada();

        List<List<Recorrido>> recorridos = coordinador.calcularRecorrido(origen, destino, dia, hora);

        resultado(recorridos, origen, destino, hora);
    }
    public void setCoordinador(CoordinadorApp coordinador){
        this.coordinador = coordinador;
    }
    /**
     * Permite al usuario ingresar el código de la parada de origen.
     * Valida que la parada exista en el mapa de paradas.
     * 
     * @param paradas Mapa de todas las paradas disponibles (código → Parada)
     * @return La parada seleccionada por el usuario
     */
    public Parada ingresarParadaOrigen(Map<Integer, Parada> paradas) {
        if(debug)return paradas.get(origen);
        System.out.println("=== SELECCIÓN DE PARADA ORIGEN ===");
        mostrarParadasDisponibles(paradas);

        while (true) {
            System.out.print("Ingrese el código de la parada de origen: ");
            int codigo = leerEnteroSeguro();

            Parada parada = paradas.get(codigo);
            if (parada != null) {
                return parada;
            } else {
                LOGGER.error("ingresarParadaOrigen: ❌ No existe una parada con ese código. Intente nuevamente.\n");
            }
        }
        
    }

    /**
     * Permite al usuario ingresar el código de la parada destino.
     * Valida que la parada exista en el mapa de paradas.
     * 
     * @param paradas Mapa de todas las paradas disponibles (código → Parada)
     * @return La parada seleccionada por el usuario
     */
    public Parada ingresarParadaDestino(Map<Integer, Parada> paradas) {
        if(debug)return paradas.get(destino);

        System.out.println("=== SELECCIÓN DE PARADA DESTINO ===");
        mostrarParadasDisponibles(paradas);

        while (true) {
            System.out.print("Ingrese el código de la parada destino: ");
            int codigo = leerEnteroSeguro();

            Parada parada = paradas.get(codigo);
            if (parada != null) {
                return parada;
            } else {
                System.out.println("❌ No existe una parada con ese código. Intente nuevamente.\n");
            }
        }
        
    }

    /**
     * Permite al usuario ingresar el día de la semana (1 = Lunes ... 7 = Domingo)
     * 
     * @return Número correspondiente al día de la semana.
     */
    public int ingresarDiaSemana() {
        if(debug) return 1;
        
        System.out.println("=== INGRESO DE DÍA DE LA SEMANA ===");
        System.out.print("Ingrese el día de la semana (1 = Lunes ... 7 = Domingo): ");

        int dia = leerEnteroSeguro();
        while (dia < 1 || dia > 7) {
            LOGGER.warn("ingresarDiaSemana: Valor inválido. Ingrese un número entre 1 y 7");
            dia = leerEnteroSeguro();
        }
        return dia;
    }

    /**
     * Permite al usuario ingresar la hora en que llega a la parada (HH:mm)
     * 
     * @return Hora de llegada como LocalTime.
     */
    public LocalTime ingresarHoraLlegaParada() {
        if(debug) return LocalTime.of(10,35);

        System.out.println("=== INGRESO DE HORA ===");
        System.out.print("Ingrese la hora de llegada a la parada (formato HH:mm): ");

        while (true) {
            try {
                String input = sc.next();
                return LocalTime.parse(input);
            } catch (Exception e) {
                LOGGER.error("ingresarHoraLlegaParada: Formato inválido. Intente nuevamente (ejemplo 10:35): ");
            }
        }
    }

    /**
     * Muestra los resultados de los recorridos calculados.
     */
    public void resultado(List<List<Recorrido>> listaRecorridos,
								Parada paradaOrigen,
								Parada paradaDestino,
								LocalTime horaLlegaParada) {

        System.out.println("\n==============================");
        System.out.println("Parada origen: " + paradaOrigen);
        System.out.println("Parada destino: " + paradaDestino);
        System.out.println("Hora llegada pasajero: " + horaLlegaParada);
        System.out.println("==============================");

        if (listaRecorridos.isEmpty()) {
            LOGGER.error("resultado: No hay recorridos recomendado");
            return;
        }
        for (List<Recorrido> recorridos : listaRecorridos) {
            for (Recorrido r : recorridos) {
                String mensajeLinea = r.getLinea() != null ? "Línea: " + r.getLinea().getNombre() : "Caminando" ;
                System.out.println(mensajeLinea);
                System.out.println("Paradas: " + r.getParadas());
                System.out.println("Hora de salida: " + r.getHoraSalida());
                System.out.println("Duración: " + Tiempo.segundosATiempo(r.getDuracion()));
                System.out.println("==============================");
            }
            System.out.println("Duración total: " + Tiempo.calcularDuracionTotalViaje(recorridos.getLast(), horaLlegaParada));
            System.out.println("Hora de llegada: " + Tiempo.calcularHoraLlegadaDestino(recorridos.getLast()));
            System.out.println("==============================");
        }
    }

    // Muestra todas las paradas disponibles
    private void mostrarParadasDisponibles(Map<Integer, Parada> paradas) {
        System.out.println("Paradas disponibles:");
        for (Parada p : paradas.values()) {
            System.out.println("Código: " + p.getCodigo() + " → " + p.getDireccion());
        }
        System.out.println();
    }

    // Lee un entero de forma segura
    private int leerEnteroSeguro() {
        while (true) {
            try {
                return sc.nextInt();
            } catch (InputMismatchException e) {
                System.out.print("Entrada inválida. Ingrese un número entero: ");
                sc.nextLine();
            }
        }
    }

    public void setDebug(boolean debug) {
        InterfazConsola.debug = debug;
    }
}
