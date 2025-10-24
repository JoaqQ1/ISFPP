package colectivo.servicio;

import java.util.Map;

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Parada;

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
