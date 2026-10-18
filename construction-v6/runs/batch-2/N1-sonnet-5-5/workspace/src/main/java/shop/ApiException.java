package shop;

/** A refusal reported to the caller as {"error": code} with an HTTP status. */
public class ApiException extends RuntimeException {
    private final String code;
    private final int status;

    private ApiException(String code, int status) {
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

    public static ApiException unavailable(String code) {
        return new ApiException(code, 503);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
