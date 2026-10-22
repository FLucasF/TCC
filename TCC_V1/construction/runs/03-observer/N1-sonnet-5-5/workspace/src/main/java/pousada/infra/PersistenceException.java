package pousada.infra;

public class PersistenceException extends RuntimeException {
    public PersistenceException(Throwable cause) {
        super(cause);
    }
}
