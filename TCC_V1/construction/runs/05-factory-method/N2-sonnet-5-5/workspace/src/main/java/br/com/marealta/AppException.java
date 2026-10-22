package br.com.marealta;

public class AppException extends RuntimeException {
    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }

    public String code() { return code; }

    public static AppException invalid(String message) {
        return new AppException(400, "INVALID_REQUEST", message);
    }

    public static AppException notAllowed(String message) {
        return new AppException(409, "OPERATION_NOT_ALLOWED", message);
    }
}
