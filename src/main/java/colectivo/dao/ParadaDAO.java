package colectivo.dao;

import java.util.Map;

import colectivo.modelo.Parada;

public interface ParadaDAO {

    // public void insertar( Parada parada );

    // public void actualizar( Parada parada );

    // public void borrar( Parada parada );

    public Map<Integer,Parada> buscarTodos();
}
