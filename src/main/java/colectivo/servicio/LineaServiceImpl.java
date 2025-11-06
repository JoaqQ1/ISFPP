package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Linea;
import colectivo.persistencia.dao.LineaDAO;

public class LineaServiceImpl implements LineaService {

	ConfiguracionGlobal config;
	private LineaDAO lineaDAO; 
		
	public LineaServiceImpl(){
		config = ConfiguracionGlobal.getConfiguracionGlobal();
		if(config.getPersistenciaTipo().equals(Constantes.BD)){
			lineaDAO = (LineaDAO) Factory.getInstancia(Constantes.LINEA_BD, LineaDAO.class);
		}else{
			lineaDAO = (LineaDAO) Factory.getInstancia(Constantes.LINEA, LineaDAO.class);
		}
	}
	public Map<String,Linea> buscarTodos() {
		return lineaDAO.buscarTodos();
	}
	
}
