package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
    private BigDecimal amountPaid = Money.round(BigDecimal.ZERO);
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
        BigDecimal sum = Money.round(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(Season.priceOfNight(room, night));
        }
        this.subtotal = sum;
        this.discount = ratePlan.discount(sum);
        this.total = sum.subtract(discount);
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
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    /** Null enquanto a reserva não for cancelada. */
    public BigDecimal refund() { return refund; }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    void confirm() {
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    void markCheckedIn() { status = ReservationStatus.CHECKED_IN; }

    void markCheckedOut() { status = ReservationStatus.CHECKED_OUT; }

    void cancel() {
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }
}
