package colectivo.datos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;

public class CargarDatos {

	public static Map<Integer, Parada> cargarParadas(String nombreArchivo) throws IOException {

		return null;
	}

	public static Map<String, Tramo> cargarTramos(String nombreArchivo, Map<Integer, Parada> paradas)
			throws FileNotFoundException {
		return null;
	}

	public static Map<String, Linea> cargarLineas(String nombreArchivo, String nombreArchivoFrecuencia,
			Map<Integer, Parada> paradas) throws FileNotFoundException {
		return null;
	}

}
