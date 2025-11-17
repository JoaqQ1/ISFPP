package colectivo.excepciones;

/**
 * Excepción base para todos los errores controlados de la aplicación.
 * Al ser una 'checked exception', obliga a quien la llama a manejarla.
 */
public class ColectivoException extends RuntimeException {
    public ColectivoException(String message) {
        super(message);
    }

    public ColectivoException(String message, Throwable cause) {
        super(message, cause);
    }
}