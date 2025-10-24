package colectivo.servicio;

import java.util.Map;

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.dao.LineaDAO;
import colectivo.modelo.Linea;

public class LineaServiceImpl implements LineaService {

	private LineaDAO lineaDAO; 
		
	public LineaServiceImpl(){
		lineaDAO = (LineaDAO) Factory.getInstancia(Constantes.LINEA);
	}
	public Map<String,Linea> buscarTodos() {
		return lineaDAO.buscarTodos();
	}
	
}
