package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final Pricing.Quote quote;
    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Money.ZERO;
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
        this.quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return quote.nights(); }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** Money kept by the inn: paid minus refunded. */
    public BigDecimal netAmount() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    /** True if the stay covers the given night (check-in night counts, check-out day does not). */
    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate in, LocalDate out) {
        return checkIn.isBefore(out) && in.isBefore(checkOut);
    }

    public void confirm() {
        requireStatus(ReservationStatus.PENDING);
        amountPaid = total();
        status = ReservationStatus.CONFIRMED;
    }

    public void startStay() {
        requireStatus(ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        requireStatus(ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Reserva com status " + status + " não pode ser cancelada");
        }
        refund = ratePlan == RatePlan.STANDARD ? amountPaid : Money.ZERO;
        status = ReservationStatus.CANCELLED;
    }

    private void requireStatus(ReservationStatus expected) {
        if (status != expected) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED",
                    "Operação exige reserva " + expected + ", mas ela está " + status);
        }
    }
}
