package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private long id;
    private final int roomNumber;
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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(Room room, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Pricing.Quote q = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(0, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, q.nights(), q.subtotal(), q.discount(), q.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED, "Só uma reserva pendente pode ser confirmada");
        amountPaid = total;
    }

    public void doCheckIn() {
        moveTo(ReservationStatus.CHECKED_IN, "Só uma reserva confirmada pode fazer check-in");
    }

    public void doCheckOut() {
        moveTo(ReservationStatus.CHECKED_OUT, "Só uma reserva com check-in pode fazer check-out");
    }

    public void cancel() {
        moveTo(ReservationStatus.CANCELLED, "Só uma reserva pendente ou confirmada pode ser cancelada");
        refund = ratePlan.refundFor(amountPaid);
    }

    private void moveTo(ReservationStatus next, String refusal) {
        if (!status.canGoTo(next)) {
            throw AppException.notAllowed(refusal);
        }
        status = next;
    }

    /** Dinheiro que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void assignId(long id) {
        this.id = id;
    }

    public long id() { return id; }
    public int roomNumber() { return roomNumber; }
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
    public BigDecimal refund() { return refund; }
}
