package br.com.marealta.domain;

public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    public static BusinessException notAllowed(String message) {
        return new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, message);
    }
}
