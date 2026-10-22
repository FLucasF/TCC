package br.com.marealta.domain;

/** Erro de negócio com o status HTTP e o código que o site espera. */
public class ApiException extends RuntimeException {
    public final int status;
    public final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException invalid(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public static ApiException notAllowed(String message) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", message);
    }
}
