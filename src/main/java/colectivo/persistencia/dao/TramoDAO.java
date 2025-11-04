package colectivo.persistencia.dao;

import java.util.Map;

import colectivo.modelo.Tramo;

public interface TramoDAO {

    // public void insertar( Tramo tramo );

    // public void actualizar( Tramo tramo );

    // public void borrar( Tramo tramo );

    public Map<String,Tramo> buscarTodos();
}
