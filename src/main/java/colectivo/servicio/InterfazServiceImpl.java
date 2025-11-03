package colectivo.servicio;

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.controlador.CoordinadorApp;
import colectivo.interfaz.Interfaz;

public class InterfazServiceImpl implements InterfazService{
    
    private Interfaz interfaz;
    
    
    public InterfazServiceImpl() {
        interfaz = (Interfaz) Factory.getInstancia(Constantes.INTERFAZ);
    }

    public void iniciar(){
        interfaz.iniciar();
    }
    public void setCoordinador(CoordinadorApp coordinador){
        interfaz.setCoordinador(coordinador);
    }
}
