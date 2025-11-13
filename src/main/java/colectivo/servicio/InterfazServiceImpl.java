package colectivo.servicio;

import colectivo.configuracion.Factory;
import colectivo.constantes.Constantes;
import colectivo.controlador.CoordinadorApp;
import colectivo.ui.Interfaz;

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
