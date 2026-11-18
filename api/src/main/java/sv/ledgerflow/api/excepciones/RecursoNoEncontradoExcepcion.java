package sv.ledgerflow.api.excepciones;

public class RecursoNoEncontradoExcepcion extends RuntimeException {
    public RecursoNoEncontradoExcepcion(String message) {
        super(message);
    }
}
