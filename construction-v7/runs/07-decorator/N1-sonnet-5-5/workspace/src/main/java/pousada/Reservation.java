package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static pousada.Money.round;

public class Reservation {
    public final int id;
    public final Room room;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;

    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = round(BigDecimal.ZERO);
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
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
        this.subtotal = Pricing.subtotal(room.type(), checkIn, checkOut);
        this.discount = ratePlan.discountOn(subtotal);
        this.total = round(subtotal.subtract(discount));
    }

    public ReservationStatus status() {
        return status;
    }

    public BigDecimal amountPaid() {
        return amountPaid;
    }

    public BigDecimal refund() {
        return refund;
    }

    /** Dinheiro que fica com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return round(amountPaid.subtract(refund == null ? BigDecimal.ZERO : refund));
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        refund = ratePlan.refundOn(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw ApiException.notAllowed(message);
        }
    }
}
