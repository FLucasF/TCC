package com.marealta;

/** Erro de negócio/validação que vira { "error": code, "message": ... } com o status HTTP indicado. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException invalid(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public int status() { return status; }
    public String code() { return code; }
}
