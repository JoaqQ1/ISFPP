package colectivo.excepciones;

public class ConexionException extends ColectivoException {
    public ConexionException(String message) {
        super(message);
    }

    public ConexionException(String message, Throwable cause) {
        super(message, cause);
    }
}
