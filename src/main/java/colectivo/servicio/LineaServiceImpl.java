package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Linea;
import colectivo.persistencia.dao.LineaDAO;

public class LineaServiceImpl implements LineaService {

	private LineaDAO lineaDAO; 
		
	public LineaServiceImpl(){
		lineaDAO = (LineaDAO) Factory.getInstancia(Constantes.LINEA, LineaDAO.class);
	}
	public Map<String,Linea> buscarTodos() {
		return lineaDAO.buscarTodos();
	}
	
}
