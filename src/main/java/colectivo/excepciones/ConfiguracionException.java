package colectivo.excepciones;

/**
 * Se lanza cuando hay un error crítico de configuración
 * (ej. no se encuentra un archivo, falta una propiedad).
 */
public class ConfiguracionException extends ColectivoException {
    public ConfiguracionException(String message) {
        super(message);
    }

    public ConfiguracionException(String message, Throwable cause) {
        super(message, cause);
    }
}