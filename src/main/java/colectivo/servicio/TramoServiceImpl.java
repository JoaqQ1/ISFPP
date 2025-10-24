package colectivo.servicio;

import java.util.Map;

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Tramo;

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
