package com.marealta;

public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }

    public static ApiException invalid(String message) { return new ApiException(400, "INVALID_REQUEST", message); }
    public static ApiException roomNotFound(int number) { return new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe"); }
    public static ApiException reservationNotFound(String id) { return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"); }
    public static ApiException roomUnavailable(int number) { return new ApiException(409, "ROOM_UNAVAILABLE", "Quarto " + number + " já reservado em alguma das noites"); }
    public static ApiException notAllowed(String message) { return new ApiException(409, "OPERATION_NOT_ALLOWED", message); }
    public static ApiException capacityExceeded(Room room) { return new ApiException(422, "CAPACITY_EXCEEDED", "Quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes"); }
}
