package br.com.marealta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tipos de domínio da pousada. */
public final class Domain {
    private Domain() {}

    public enum RatePlan { STANDARD, NON_REFUNDABLE }

    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

        /** Reserva paga: confirmada, com check-in ou com check-out. */
        public boolean isPaid() {
            return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
        }
    }

    public record Room(int number, String type, int capacity, BigDecimal nightlyRate) {}

    public record Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
                              BigDecimal amountPaid, BigDecimal refund) {}

    /** Dados já validados de um pedido de reserva. */
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public enum MessageStatus { SENT, BLOCKED }

    public record Message(long id, String channel, String to, String subject, String body, MessageStatus status) {}
}
