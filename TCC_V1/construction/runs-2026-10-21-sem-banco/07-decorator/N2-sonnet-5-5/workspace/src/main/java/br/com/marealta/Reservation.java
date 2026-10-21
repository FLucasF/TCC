package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(Pricing.nightPrice(room, night));
        }
        this.subtotal = Money.round(sum);
        this.discount = ratePlan.discountOn(subtotal);
        this.total = Money.round(subtotal.subtract(discount));
    }

    int id() { return id; }
    Room room() { return room; }
    String guestName() { return guestName; }
    String guestEmail() { return guestEmail; }
    String guestCpf() { return guestCpf; }
    LocalDate checkIn() { return checkIn; }
    LocalDate checkOut() { return checkOut; }
    int guests() { return guests; }
    RatePlan ratePlan() { return ratePlan; }
    int nights() { return nights; }
    BigDecimal subtotal() { return subtotal; }
    BigDecimal discount() { return discount; }
    BigDecimal total() { return total; }
    ReservationStatus status() { return status; }
    BigDecimal amountPaid() { return amountPaid; }
    BigDecimal refund() { return refund; }

    boolean isActive() { return status != ReservationStatus.CANCELLED; }

    boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOut);
    }

    boolean occupiesNight(LocalDate night) {
        return status.isPaid() && !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** O que a pousada ficou: pago menos devolvido. */
    BigDecimal netAmount() {
        return Money.round(refund == null ? amountPaid : amountPaid.subtract(refund));
    }

    void confirm() {
        require("confirmar", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    void doCheckIn() {
        require("fazer check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    void doCheckOut() {
        require("fazer check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    void cancel() {
        require("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    Receipt receipt() {
        if (!status.isPaid()) {
            throw DomainException.notAllowed("Reserva " + id + " não tem pagamento; não há comprovante");
        }
        return new Receipt("Comprovante de reserva", id, guestName, guestCpf, room.number(), room.type(),
                checkIn, checkOut, nights, subtotal, discount, total);
    }

    private void require(String operation, ReservationStatus... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw DomainException.notAllowed("Não é possível " + operation + " uma reserva com situação " + status);
        }
    }
}
