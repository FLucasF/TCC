package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tipos básicos do domínio da pousada. */
public final class Domain {
    private Domain() {}

    public enum RoomType { STANDARD, DELUXE, SUITE }

    public enum RatePlan { STANDARD, NON_REFUNDABLE }

    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

        /** Reserva paga: confirmada, com check-in ou com check-out. */
        public boolean isPaid() {
            return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
        }
    }

    public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    public record Reservation(
            long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
            BigDecimal amountPaid, BigDecimal refund) {}
}
