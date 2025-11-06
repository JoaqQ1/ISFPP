package colectivo.modelo;

import java.util.ArrayList;
import java.util.List;

public class Parada {

	private int codigo;
	private String direccion;
	private List<Linea> lineas;
	private List<Parada> paradaCaminando;
	private double latitud;
	private double longitud;

	public Parada() {
		this.lineas = new ArrayList<Linea>();
		this.paradaCaminando = new ArrayList<Parada>();
	}

	public Parada(int codigo, String direccion, double latitud, double longitud) {
		super();
		this.codigo = codigo;
		this.direccion = direccion;
		this.latitud = latitud;
		this.longitud = longitud;
		this.lineas = new ArrayList<Linea>();
		this.paradaCaminando = new ArrayList<Parada>();
	}

	public void agregarLinea(Linea linea) { 
		// Agrega la linea solo si no está ya en la lista
		if(!this.lineas.contains(linea)) {
			this.lineas.add(linea);
		}
	}

	public void agregarParadaCaminado(Parada parada) {
		this.paradaCaminando.add(parada);
	}

	public int getCodigo() {
		return codigo;
	}

	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public double getLatitud() {
		return latitud;
	}

	public void setLatitud(double latitud) {
		this.latitud = latitud;
	}

	public double getLongitud() {
		return longitud;
	}

	public void setLongitud(double longitud) {
		this.longitud = longitud;
	}

	public List<Linea> getLineas() {
		return lineas;
	}
		
	public List<Parada> getParadaCaminando() {
		return paradaCaminando;
	}

	@Override
	public String toString() {
		return "Parada [codigo=" + codigo + ", direccion=" + direccion + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + codigo;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parada other = (Parada) obj;
		if (codigo != other.codigo)
			return false;
		return true;
	}

}
