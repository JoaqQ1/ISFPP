package colectivo.modelo;

import java.time.LocalTime;
import java.util.List;

public class Recorrido {

	private Linea linea;
	private List<Parada> paradas;
	private LocalTime horaSalida;
	private LocalTime horaSalidaColectivo;
	private int duracion;

	public Recorrido(Linea linea, List<Parada> paradas, LocalTime horaSalida, LocalTime horaSalidaColectivo, int duracion) {
		super();
		this.linea = linea;
		this.paradas = paradas;
		this.horaSalida = horaSalida;
		this.horaSalidaColectivo = horaSalidaColectivo;
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
	
	public LocalTime getHoraSalidaColectivo() {
		return horaSalidaColectivo;
	}
	
	public void setHoraSalidaColectivo(LocalTime horaSalidaColectivo) {
		this.horaSalidaColectivo = horaSalidaColectivo;
	}
	
	public LocalTime getHoraLlegada() {
		return this.horaSalida.plusSeconds(duracion);
	}
	
}
