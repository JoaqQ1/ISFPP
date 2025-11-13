package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Parada;
import colectivo.persistencia.dao.ParadaDAO;

public class ParadaServiceImpl implements ParadaService{
    private ParadaDAO paradaDAO;
    public ParadaServiceImpl(){
        paradaDAO = (ParadaDAO) Factory.getInstancia(Constantes.PARADA);
   
    }

	@Override
	public Map<Integer,Parada> buscarTodos() {
		return paradaDAO.buscarTodos();
		
	}

}
