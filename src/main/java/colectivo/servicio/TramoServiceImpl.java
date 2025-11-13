package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Tramo;
import colectivo.persistencia.dao.TramoDAO;

public class TramoServiceImpl implements TramoService{
    private TramoDAO tramoDAO;
    public TramoServiceImpl(){
        tramoDAO = (TramoDAO) Factory.getInstancia(Constantes.TRAMO);

    }
    @Override
    public Map<String, Tramo> buscarTodos() {
        return tramoDAO.buscarTodos();
    }

}
