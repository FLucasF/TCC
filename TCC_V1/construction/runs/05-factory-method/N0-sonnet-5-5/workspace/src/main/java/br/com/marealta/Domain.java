package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

enum RoomType { STANDARD, DELUXE, SUITE }

enum RatePlan { STANDARD, NON_REFUNDABLE }

enum Status {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}

record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

/** Como o hóspede se identifica. Cada tipo gera o seu próprio documento (ver Documents). */
sealed interface Identification {
    record Cpf(String cpf) implements Identification {}
    record Company(String cnpj, String companyName) implements Identification {}
    record Passport(String passport, String country) implements Identification {}
}

record Reservation(
        long id, int roomNumber, String guestName, String guestEmail, Identification identification,
        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
        Status status, BigDecimal amountPaid, BigDecimal refund) {}

record Message(long id, String channel, String to, String subject, String body) {}

/** Erro de negócio/validação que vira uma resposta { error, message }. */
class ApiException extends RuntimeException {
    final int status;
    final String code;

    ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    static ApiException invalid(String message) { return new ApiException(400, "INVALID_REQUEST", message); }
    static ApiException notAllowed(String message) { return new ApiException(409, "OPERATION_NOT_ALLOWED", message); }
}
