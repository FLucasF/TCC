package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Domain {
    private Domain() {}

    public enum RoomType { STANDARD, DELUXE, SUITE }

    public enum RatePlan { STANDARD, NON_REFUNDABLE }

    public enum Status { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    public record Reservation(
            long id, int roomNumber, String guestName, String guestEmail, Identification identification,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
            int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
            Status status, BigDecimal amountPaid, BigDecimal refund) {

        /** Reserva paga: confirmada, com check-in ou com check-out. */
        public boolean isPaid() {
            return status == Status.CONFIRMED || status == Status.CHECKED_IN || status == Status.CHECKED_OUT;
        }
    }

    public record Message(long id, String channel, String to, String subject, String body) {}
}
