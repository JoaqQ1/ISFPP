package colectivo.servicio;

import java.util.Map;

import colectivo.conexion.Factory;
import colectivo.dao.TramoDAO;
import colectivo.modelo.Tramo;

public class TramoServiceImpl implements TramoService{
    private TramoDAO tramoDAO;
    public TramoServiceImpl(){
        tramoDAO = (TramoDAO) Factory.getInstancia("TRAMO");
    }
    @Override
    public Map<String, Tramo> buscarTodos() {
        return tramoDAO.buscarTodos();
    }
    
}
