package colectivo.servicio;

import colectivo.conexion.Factory;
import colectivo.controlador.Constantes;
import colectivo.controlador.Coordinador;
import colectivo.interfaz.Interfaz;

public class InterfazServiceImpl implements InterfazService{
    
    private Interfaz interfaz;
    
    
    public InterfazServiceImpl() {
        interfaz = (Interfaz) Factory.getInstancia(Constantes.INTERFAZ);
    }

    public void iniciar(){
        interfaz.iniciar();
    }
    public void setCoordinador(Coordinador coordinador){
        interfaz.setCoordinador(coordinador);
    }
}
