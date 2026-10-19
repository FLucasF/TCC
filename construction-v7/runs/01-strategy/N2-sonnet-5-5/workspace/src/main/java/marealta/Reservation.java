package marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

import static marealta.ReservationStatus.*;

public class Reservation {
    private final int id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan plan;
    private final int nights;
    private final Quote quote;

    private ReservationStatus status = PENDING;
    private BigDecimal amountPaid = Money.ZERO;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan, Quote quote) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.plan = plan;
        this.quote = quote;
        this.nights = new Stay(room, checkIn, checkOut, null).nights();
    }

    public void confirm() {
        require("confirmar", PENDING);
        status = CONFIRMED;
        amountPaid = total();
    }

    public void checkIn() {
        require("fazer check-in de", CONFIRMED);
        status = CHECKED_IN;
    }

    public void checkOut() {
        require("fazer check-out de", CHECKED_IN);
        status = CHECKED_OUT;
    }

    public void cancel() {
        require("cancelar", PENDING, CONFIRMED);
        refund = plan.refund(amountPaid);
        status = CANCELLED;
    }

    private void require(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) return;
        }
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva " + status);
    }

    public boolean isPaid() {
        return status == CONFIRMED || status == CHECKED_IN || status == CHECKED_OUT;
    }

    public boolean isActive() { return status != CANCELLED; }

    public boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
        return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    /** Dinheiro que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public int id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan plan() { return plan; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return quote.subtotal(); }
    public BigDecimal discount() { return quote.discount(); }
    public BigDecimal total() { return quote.total(); }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
