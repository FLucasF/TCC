package br.com.marealta.pousada.service;

public class DomainException extends RuntimeException {
    private final int status;
    private final String code;

    public DomainException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static DomainException invalid(String message) {
        return new DomainException(400, "INVALID_REQUEST", message);
    }
}
