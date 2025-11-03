package colectivo.dao.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import colectivo.conexion.ConexionBD;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Parada;

public class ParadaBdDAO implements ParadaDAO{
	
	private Map<Integer, Parada> paradas;
	private boolean actualizar;
    private Connection conexion;	
	
    public ParadaBdDAO() {
    	this.paradas = null;
		this.actualizar = true;
		this.conexion = ConexionBD.getConnection();
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

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return paradasBd;
	}

}
