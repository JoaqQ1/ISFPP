package colectivo.interfaz;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.util.Tiempo;

public class Interfaz {

	// Usuario ingresa parada origen
	public static Parada ingresarParadaOrigen(Map<Integer, Parada> paradas) {
		return paradas.get(44);
	}

	// Usuario ingresa parada destino
	public static Parada ingresarParadaDestino(Map<Integer, Parada> paradas) {
		return paradas.get(47);
	}

	// Usuario ingresa d�a de la semana (1=lunes, 2=martes, ... 7=domingo)
	public static int ingresarDiaSemana() {
		return 1;
	}

	// Usuario ingresa hora de llegada a la parada
	public static LocalTime ingresarHoraLlegaParada() {
		return LocalTime.of(10, 35);
	}

	// Mostrar los resultados
	public static void resultado(List<List<Recorrido>> listaRecorridos, Parada paradaOrigen, Parada paradaDestino,
			LocalTime horaLlegaParada) {
				
		System.out.println("Parada origen: " + paradaOrigen);		
		System.out.println("Parada destino: " + paradaDestino);
		System.out.println("Llega a la parada: " + horaLlegaParada);

		if(listaRecorridos.isEmpty()){
			System.out.println("No hay recorridos disponibles");
			return;
		}

		System.out.println("============================");
		for(List<Recorrido> recorridos:listaRecorridos){
			for(Recorrido r: recorridos){
				System.out.println("Linea: " + r.getLinea().getNombre());
				System.out.println("Paradas: " + r.getParadas());
				System.out.println("Hora de salida: " + r.getHoraSalida());
				System.out.println("Duración: " + Tiempo.segundosATiempo(r.getDuracion()));
				System.out.println("============================");
				System.out.println("Duración total: " + Tiempo.calcularDuracionTotalViaje(r, horaLlegaParada));
				System.out.println("Hora de llegada: " + Tiempo.calcularHoraLlegadaDestino(r));
				System.out.println("============================");
			}
		}		
		System.out.println();
	}

}
