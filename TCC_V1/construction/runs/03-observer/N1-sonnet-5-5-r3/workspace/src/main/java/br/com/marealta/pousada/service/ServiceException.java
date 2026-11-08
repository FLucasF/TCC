package br.com.marealta.pousada.service;

public class ServiceException extends RuntimeException {

    private final int status;
    private final String code;

    public ServiceException(int status, String code, String message) {
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

    public static ServiceException invalid(String message) {
        return new ServiceException(400, "INVALID_REQUEST", message);
    }
}
