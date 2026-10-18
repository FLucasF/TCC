package shop.domain;

/** A refusal that the API reports as {"error": code} with the given HTTP status. */
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
