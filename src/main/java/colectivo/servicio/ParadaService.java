package colectivo.servicio;

import java.util.Map;

import colectivo.modelo.Parada;

public interface ParadaService {
    // void insertar(Parada parada);

	// void actualizar(Parada parada);

	// void borrar(Parada parada);

	Map<Integer,Parada> buscarTodos();
}
