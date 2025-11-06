package colectivo.persistencia.dao.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import colectivo.conexion.ConexionBD;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.ParadaDAO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class ParadaBdDAO implements ParadaDAO{

	private static final Logger LOGGER = LogManager.getLogger(ParadaBdDAO.class.getName());
	private Map<Integer, Parada> paradas;
	private boolean actualizar;
    private Connection conexion;	
	
    public ParadaBdDAO() {
    	this.paradas = null;
		this.actualizar = true;
		this.conexion = ConexionBD.getConnection();
		LOGGER.info("ParadaBdDAO iniciada con conexión a base de datos.");
	}
	
	public Map<Integer, Parada> buscarTodos() {
		if(paradas == null || actualizar) {
			paradas = leerBD();
			actualizar = false;
		}		
		return paradas;
	}
	
	private Map<Integer, Parada> leerBD() {
		Map<Integer, Parada> paradasBd = new TreeMap<>();
		
		String query = """
				SELECT *
				FROM parada
				ORDER BY codigo;
				""";

		try (Statement stmt = conexion.createStatement();
				ResultSet rs = stmt.executeQuery(query);) {

			while (rs.next()) {
				Integer codigo = rs.getInt("codigo");
				String direccion = rs.getString("direccion");
				double latitud = rs.getDouble("latitud");
				double longitud = rs.getDouble("longitud");
				
				Parada parada = new Parada(codigo, direccion, latitud, longitud);
				paradasBd.put(codigo, parada);
			}
			LOGGER.info("Paradas cargadas desde base de datos.");
		} catch (SQLException e) {
			LOGGER.error("leerBD: Error cargando paradas desde base de datos.", e);
		}
		return paradasBd;
	}

}