package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.ParadaDAO;

public class ParadaServiceImpl implements ParadaService{
    ConfiguracionGlobal config;
    private ParadaDAO paradaDAO;
    public ParadaServiceImpl(){
        config = ConfiguracionGlobal.getConfiguracionGlobal();
        if(config.getPersistenciaTipo().equals(Constantes.BD)){
            paradaDAO = (ParadaDAO) Factory.getInstancia(Constantes.PARADA_BD, ParadaDAO.class);
        }else{
            paradaDAO = (ParadaDAO) Factory.getInstancia(Constantes.PARADA, ParadaDAO.class);
        }
    }

	@Override
	public Map<Integer,Parada> buscarTodos() {
		return paradaDAO.buscarTodos();
		
	}

}
