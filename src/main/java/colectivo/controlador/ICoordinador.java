package colectivo.controlador;

import colectivo.modelo.Linea;
import colectivo.modelo.Parada;
import colectivo.modelo.Recorrido;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public interface ICoordinador {
    // Métodos de Datos
    Map<Integer, Parada> listarParadas();
    Map<String, Linea> listarLineas();
    
    // Métodos de Cálculo
    List<List<Recorrido>> calcularRecorrido(Parada origen, Parada destino, int dia, LocalTime hora);
    
    // Configuración
    double getOrigenLatitud();
    double getOrigenLongitud();
    int getZoom();
    ResourceBundle getResourceBundle();
    void setIdioma(Locale locale);
    String getIdiomaActual();
}