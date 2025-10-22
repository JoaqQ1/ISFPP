package colectivo.servicio;

import java.util.Map;

import colectivo.conexion.Factory;
import colectivo.dao.ParadaDAO;
import colectivo.modelo.Parada;

public class ParadaServiceImpl implements ParadaService{
    private ParadaDAO paradaDAO;
    public ParadaServiceImpl(){
        paradaDAO = (ParadaDAO) Factory.getInstancia("PARADA");
    }

	@Override
	public Map<Integer,Parada> buscarTodos() {
		return paradaDAO.buscarTodos();
		
	}
}
