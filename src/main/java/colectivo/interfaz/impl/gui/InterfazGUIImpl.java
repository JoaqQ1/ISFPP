package colectivo.interfaz.impl.gui; // Usamos un subpaquete para distinguir de la clase Application

import colectivo.controlador.CoordinadorApp;
import colectivo.interfaz.Interfaz;

/**
 * Implementación de la Interfaz que el Factory cargará para iniciar la GUI.
 */
public class InterfazGUIImpl implements Interfaz {

    private CoordinadorApp coordinador;

    @Override
    public void setCoordinador(CoordinadorApp coordinador) {
        this.coordinador = coordinador;
    }

    @Override
    public void iniciar() {
        
        InterfazGrafica.launchApp(coordinador, new String[]{});
    }
}