package pousada.error;

/** Erro de negócio ou de entrada que vira resposta { "error", "message" }. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    private ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public static ApiException invalidRequest(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    public static ApiException roomNotFound() {
        return new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado");
    }

    public static ApiException reservationNotFound() {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }

    public static ApiException roomUnavailable() {
        return new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
    }

    public static ApiException operationNotAllowed(String message) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", message);
    }

    public static ApiException capacityExceeded() {
        return new ApiException(422, "CAPACITY_EXCEEDED", "Mais hóspedes do que o quarto comporta");
    }
}
