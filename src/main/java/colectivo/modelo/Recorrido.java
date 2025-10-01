package colectivo.modelo;

import java.time.LocalTime;
import java.util.List;

public class Recorrido {

	private Linea linea;
	private List<Parada> paradas;
	private LocalTime horaSalida;
	private int duracion;

	public Recorrido(Linea linea, List<Parada> paradas, LocalTime horaSalida, int duracion) {
		super();
		this.linea = linea;
		this.paradas = paradas;
		this.horaSalida = horaSalida;
		this.duracion = duracion;
	}

	public Linea getLinea() {
		return linea;
	}

	public void setLinea(Linea linea) {
		this.linea = linea;
	}

	public List<Parada> getParadas() {
		return paradas;
	}

	public void setParadas(List<Parada> paradas) {
		this.paradas = paradas;
	}

	public LocalTime getHoraSalida() {
		return horaSalida;
	}

	public void setHoraSalida(LocalTime horaSalida) {
		this.horaSalida = horaSalida;
	}

	public int getDuracion() {
		return duracion;
	}

	public void setDuracion(int duracion) {
		this.duracion = duracion;
	}

	
}
