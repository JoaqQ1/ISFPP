package colectivo.persistencia.dao.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import colectivo.conexion.ConexionBD;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.*;
import colectivo.persistencia.dao.LineaDAO;
import colectivo.persistencia.dao.ParadaDAO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LineaBdDAO implements LineaDAO{

	private static final Logger LOGGER = LogManager.getLogger(LineaBdDAO.class.getName());
	private Map<String, Linea> lineas;
    private boolean actualizar;
    private Connection conexion;

	public LineaBdDAO() {
		this.lineas = null;
		this.actualizar = true;
		this.conexion = ConexionBD.getConnection();
		LOGGER.info("LineaBdDAO iniciada con conexión a base de datos.");
	}

	@Override
	public Map<String, Linea> buscarTodos() {
		if(lineas == null || actualizar) {
			lineas = leerBD();
			actualizar = false;
		}
		LOGGER.info("Lineas cargadas desde base de datos.");
		return lineas;
	}

	private Map<String, Linea> leerBD() {
		Map<String, Linea> lineasBd = null;
		Map<String, List<Integer>> paradasMap = null;
		Map<String, Map<Integer,List<LocalTime>>> frecuenciasMap = null;
		Map<Integer,Parada> paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();
		
		String queryParadas = """
				SELECT p.codigo, l.linea
				FROM linea_parada l
				JOIN parada p
				ON l.parada = p.codigo
				ORDER BY l.linea, l.secuencia;
				""";
		
		String queryLinea = "SELECT * FROM linea;";

		String queryFrecuencias = """
				SELECT *
				FROM linea_frecuencia
				""";

		paradasMap = cargarParadas(queryParadas);
		lineasBd = cargarLineas(queryLinea);
		frecuenciasMap = cargarFrecuencias(queryFrecuencias);
		
		// Asignar paradas y frecuencias a cada línea
		for (String codigo : lineasBd.keySet()) {
			Linea linea = lineasBd.get(codigo);
			List<Integer> codigosParadas = paradasMap.get(codigo);
			if (codigosParadas != null) {
				for (Integer codigoP : codigosParadas) {
					Parada parada = paradas.get(codigoP);
					if (parada != null) {
						linea.agregarParada(parada);
					}
				}
			}
			Map<Integer,List<LocalTime>> freqMap = frecuenciasMap.get(codigo);
			if (freqMap != null) {
				linea.agregarFrecuencias(freqMap);
			}
		}
		LOGGER.info("Lineas cargadas desde base de datos.");
		
		return lineasBd;
	}
	
	private Map<String, List<Integer>> cargarParadas(String queryParadas) {
		try (Statement stmtParadas = conexion.createStatement();
				ResultSet rsParadas = stmtParadas.executeQuery(queryParadas)) {
			Map<String, List<Integer>> paradasMap = new TreeMap<>();

			while (rsParadas.next()) {
				String lineaCodigo = rsParadas.getString("linea");
				int codigoP = rsParadas.getInt("codigo");
				
				paradasMap.computeIfAbsent(lineaCodigo, k -> new ArrayList<>()).add(codigoP);
			}
			LOGGER.info("Paradas cargadas desde base de datos.");
			return paradasMap;
		} catch (SQLException e) {
			LOGGER.error("Error cargando paradas desde base de datos.", e);
		}
		return null;
	}
	
	private Map<String, Linea> cargarLineas(String queryLinea) {
		Map<String, Linea> lineasBd = new TreeMap<>();
		try (Statement stmtLinea = conexion.createStatement(); ResultSet rsLinea = stmtLinea.executeQuery(queryLinea)) {
			Linea lineaBd = null;
			
			while (rsLinea.next()) {
				String codigo = rsLinea.getString("codigo");
				String nombre = rsLinea.getString("nombre");

				lineaBd = new Linea(codigo, nombre);
				lineasBd.put(codigo, lineaBd);
			}
			LOGGER.info("Lineas cargadas desde base de datos.");
		} catch (SQLException e) {
			LOGGER.error("Error cargando lineas desde base de datos.", e);
		}
		return lineasBd;
	}
	
	private Map<String, Map<Integer,List<LocalTime>>> cargarFrecuencias(String queryFrecuencias) {
		try (Statement stmtFrecuencias = conexion.createStatement();
				ResultSet rsFrecuencias = stmtFrecuencias.executeQuery(queryFrecuencias)) {
			Map<String, Map<Integer,List<LocalTime>>> frecuenciasMap = new TreeMap<>();

			while (rsFrecuencias.next()) {
				String lineaCodigo = rsFrecuencias.getString("linea");
				int dia = rsFrecuencias.getInt("diasemana");
				Time hora = rsFrecuencias.getTime("hora");
				LocalTime horaLT = hora.toLocalTime();
				frecuenciasMap
					.computeIfAbsent(lineaCodigo, k -> new TreeMap<>())
					.computeIfAbsent(dia, k -> new ArrayList<>())
					.add(horaLT);
			}
			LOGGER.info("Frecuencias cargadas desde base de datos.");
			return frecuenciasMap;
		} catch (SQLException e) {
			LOGGER.error("Error cargando frecuencias desde base de datos.", e);
		}
		return null;
	}
	

}