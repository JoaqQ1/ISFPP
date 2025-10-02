package colectivo.logica;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import colectivo.modelo.Tramo;

public class Calculo {

	public static List<List<Recorrido>> calcularRecorrido(Parada paradaOrigen, Parada paradaDestino, int diaSemana,
			LocalTime horaLlegaParada, Map<String, Tramo> tramos) {
		List<List<Recorrido>> listaRecorridos = new ArrayList<>();
		return listaRecorridos;
	}

}
