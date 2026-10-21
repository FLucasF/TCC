package hotel.reservations;

public final class HotelException extends RuntimeException {

    private final ErrorCode errorCode;

    public HotelException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
