package colectivo.persistencia.dao.bd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import colectivo.conexion.ConexionBD;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.excepciones.ConfiguracionException;
import colectivo.excepciones.FactoryException;
import colectivo.modelo.Parada;
import colectivo.modelo.Tramo;
import colectivo.persistencia.dao.ParadaDAO;
import colectivo.persistencia.dao.TramoDAO;
import colectivo.util.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TramoBdDAO implements TramoDAO{
	 private Map<String, Tramo> tramos;
	    private Connection conexion;
	    private boolean actualizar;
	    private static final Logger LOGGER = LogManager.getLogger(TramoBdDAO.class.getName());
	
    public TramoBdDAO() {
    	this.tramos = null;
		this.actualizar = true;
		this.conexion = ConexionBD.getConnection();
		LOGGER.info("TramoBdDAO iniciada con conexión a base de datos.");
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
		Map<Integer,Parada> paradas;
		
		String query = """
				Select *
				FROM tramo;
				""";

		try (Statement stmt = conexion.createStatement(); ResultSet rs = stmt.executeQuery(query);) {
					
			paradas = ((ParadaDAO)Factory.getInstancia(Constantes.PARADA, ParadaDAO.class)).buscarTodos();

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
			LOGGER.info("Tramos cargados desde base de datos.");
			return tramosBd;
		} catch (FactoryException | ConfiguracionException e) {
			// Error en la dependencia de Paradas
			String errorMsg = "Error al cargar Tramos: No se pudieron cargar las Paradas (dependencia).";
			LOGGER.error(errorMsg, e);
			throw new ConfiguracionException(errorMsg, e);
		} 
		catch (SQLException e) {
			String errorMsg = String.format(
				"Error de SQL al cargar 'Tramos'. Verifique la conexión, la consulta ('%s') y que la tabla 'tramo' y sus columnas (inicio, fin, tiempo, tipo) existan.",
				query
			);
			LOGGER.error(errorMsg, e);
			throw new ConfiguracionException(errorMsg, e);
		}
		
	}

}