package br.com.loja.pedidos.common;

/** A refused operation: only the error code travels to the client. */
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;

    public ApiException(String code, int status) {
        super(code, null, false, false);
        this.code = code;
        this.status = status;
    }

    public static ApiException unprocessable(String code) {
        return new ApiException(code, 422);
    }

    public static ApiException conflict(String code) {
        return new ApiException(code, 409);
    }

    public static ApiException notFound(String code) {
        return new ApiException(code, 404);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
