package br.com.mealta.pousada;

/** Erro de negócio com o status HTTP e o código que o site recebe. */
public class DomainException extends RuntimeException {
    private final int status;
    private final String code;

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static DomainException invalid(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }

    public static DomainException notAllowed(String message) {
        return new DomainException(409, "OPERATION_NOT_ALLOWED", message);
    }
}
