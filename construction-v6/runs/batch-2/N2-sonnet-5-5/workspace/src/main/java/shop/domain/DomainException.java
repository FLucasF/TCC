package shop.domain;

public class DomainException extends RuntimeException {
    private final ErrorCode code;
    private final int status;

    public DomainException(ErrorCode code) {
        this(code, code.defaultStatus());
    }

    public DomainException(ErrorCode code, int status) {
        super(code.name(), null, false, false);
        this.code = code;
        this.status = status;
    }

    public static DomainException conflict(ErrorCode code) {
        return new DomainException(code, 409);
    }

    public ErrorCode code() {
        return code;
    }

    public int status() {
        return status;
    }
}
