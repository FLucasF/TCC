package pousada.domain;

/** Erro de negócio que vira resposta { "error": CODE, "message": ... } com o status HTTP indicado. */
public class DomainException extends RuntimeException {
    private final int status;
    private final String code;

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static DomainException invalidRequest(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }

    public static DomainException operationNotAllowed(String message) {
        return new DomainException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
