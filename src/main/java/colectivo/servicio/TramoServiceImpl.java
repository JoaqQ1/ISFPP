package colectivo.servicio;

import java.util.Map;

import colectivo.configuracion.ConfiguracionGlobal;
import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.modelo.Tramo;
import colectivo.persistencia.dao.TramoDAO;

public class TramoServiceImpl implements TramoService{
    ConfiguracionGlobal config;
    private TramoDAO tramoDAO;
    public TramoServiceImpl(){
        config = ConfiguracionGlobal.getConfiguracionGlobal();
        if(config.getPersistenciaTipo().equals(Constantes.BD)){
            tramoDAO = (TramoDAO) Factory.getInstancia(Constantes.TRAMO_BD, TramoDAO.class);
        }else{
            tramoDAO = (TramoDAO) Factory.getInstancia(Constantes.TRAMO, TramoDAO.class);
        }
    }
    @Override
    public Map<String, Tramo> buscarTodos() {
        return tramoDAO.buscarTodos();
    }

}
