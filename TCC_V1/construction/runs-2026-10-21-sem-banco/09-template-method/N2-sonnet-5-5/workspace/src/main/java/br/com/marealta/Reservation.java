package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private final long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
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
        this.subtotal = Pricing.subtotal(room.type(), checkIn, checkOut);
        this.discount = ratePlan.discountOn(subtotal);
        this.total = Money.round(subtotal.subtract(discount));
    }

    public void confirm() {
        require(ReservationStatus.PENDING, "confirmar");
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        require(ReservationStatus.CONFIRMED, "fazer check-in de");
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        require(ReservationStatus.CHECKED_IN, "fazer check-out de");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw ApiException.notAllowed("Não é possível cancelar uma reserva " + status);
        }
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    private void require(ReservationStatus expected, String action) {
        if (status != expected) {
            throw ApiException.notAllowed("Não é possível " + action + " uma reserva " + status);
        }
    }

    /** Dinheiro que ficou com a pousada: o pago menos o devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public long getId() { return id; }
    public int getRoomNumber() { return room.number(); }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Room room() { return room; }
}
