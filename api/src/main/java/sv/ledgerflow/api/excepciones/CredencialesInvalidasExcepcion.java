package sv.ledgerflow.api.excepciones;

public class CredencialesInvalidasExcepcion extends RuntimeException {
    public CredencialesInvalidasExcepcion() {
        super("El correo o la contraseña no son validos");
    }
}
