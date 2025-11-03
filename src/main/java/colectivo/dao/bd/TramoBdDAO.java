package colectivo.dao.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import colectivo.conexion.ConexionBD;
import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.dao.ParadaDAO;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.util.Util;

public class TramoBdDAO implements TramoDAO{
	 private Map<String, Tramo> tramos;
	    private Connection conexion;
	    private boolean actualizar;	
	
    public TramoBdDAO() {
    	this.tramos = null;
		this.actualizar = true;
		this.conexion = ConexionBD.getConnection();
	}
	
	public Map<String, Tramo> buscarTodos() {
		if(tramos == null || actualizar) {
			tramos = leerBD();
			actualizar = false;
		}		
		return tramos;
	}
	
	private Map<String, Tramo> leerBD() {
		Map<String, Tramo> tramosBd = new TreeMap<>();
		Map<Integer,Parada> paradas = ((ParadaDAO) Factory.getInstancia(Constantes.PARADA)).buscarTodos();
		
		String query = """
				Select *
				FROM tramo;
				""";

		try (Statement stmt = conexion.createStatement();
				ResultSet rs = stmt.executeQuery(query);) {

			while (rs.next()) {
				int codigoParadaInicio = rs.getInt("inicio");
				int codigoParadaFin = rs.getInt("fin");
				int tiempo = rs.getInt("tiempo");
				int tipo = rs.getInt("tipo");
				
				Parada paradaInicio = paradas.get(codigoParadaInicio);
				Parada paradaFin = paradas.get(codigoParadaFin);
				String clave = Util.claveTramo(paradaInicio, paradaFin);
				
				Tramo tramo = new Tramo(paradaInicio, paradaFin, tiempo, tipo);
				tramosBd.put(clave, tramo);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return tramosBd;
	}

}
