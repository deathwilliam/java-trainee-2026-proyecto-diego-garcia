package sv.ledgerflow.api.excepciones;

public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String message) {
        super(message);
    }
}
