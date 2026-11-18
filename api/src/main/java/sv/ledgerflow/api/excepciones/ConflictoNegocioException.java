package sv.ledgerflow.api.excepciones;

public class ConflictoNegocioException extends RuntimeException {
    public ConflictoNegocioException(String message) {
        super(message);
    }
}
