package br.com.marealta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Domain {
    private Domain() {}

    public enum RoomType { STANDARD, DELUXE, SUITE }

    public enum RatePlan { STANDARD, NON_REFUNDABLE }

    public enum Status { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    public record Reservation(
            long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
            BigDecimal amountPaid, BigDecimal refund) {

        /** Reserva paga: confirmada, com check-in ou com check-out. */
        public boolean paid() {
            return status == Status.CONFIRMED || status == Status.CHECKED_IN || status == Status.CHECKED_OUT;
        }
    }

    public record Document(
            String title, long reservationId, String guestName, String guestCpf, int roomNumber,
            RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    public record Message(long id, String channel, String to, String subject, String body, String status) {}

    public static class ApiException extends RuntimeException {
        public final int status;
        public final String code;

        public ApiException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }
}
