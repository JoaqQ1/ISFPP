package colectivo.persistencia.dao;

import java.util.Map;

import colectivo.modelo.Linea;

public interface LineaDAO {
    
//	public void insertar( Linea linea );
	
//	public void actualizar( Linea linea );
	
//	public void borrar( Linea linea );

    public Map<String,Linea> buscarTodos( );
}
