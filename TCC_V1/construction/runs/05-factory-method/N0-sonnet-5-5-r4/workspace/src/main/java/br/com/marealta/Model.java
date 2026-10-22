package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Model {
    private Model() {}

    public enum RatePlan { STANDARD, NON_REFUNDABLE }

    public enum Status {
        PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

        /** Reserva paga: confirmada, com check-in ou com check-out. */
        public boolean isPaid() {
            return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
        }
    }

    public record Room(int number, String type, int capacity, BigDecimal nightlyRate) {}

    /** Como o hóspede se identifica; cada variante tem seu próprio documento. */
    public sealed interface Identification {
        record Cpf(String cpf) implements Identification {}
        record Company(String cnpj, String name) implements Identification {}
        record Passport(String number, String country) implements Identification {}
    }

    public record Reservation(
            long id, int roomNumber, String guestName, String guestEmail, Identification identification,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, Status status,
            BigDecimal amountPaid, BigDecimal refund) {}

    public record Message(long id, String channel, String to, String subject, String body) {}
}
