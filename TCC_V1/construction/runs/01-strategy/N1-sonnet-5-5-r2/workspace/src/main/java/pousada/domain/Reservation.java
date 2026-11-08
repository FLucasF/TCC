package pousada.domain;

import pousada.plan.RatePlan;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
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

    public Reservation withId(long newId) {
        return new Reservation(newId, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public void confirm() {
        require(ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        require(ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        require(ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    private void require(ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Operação não permitida para reserva com status " + status);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal netAmount() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public Receipt receipt() {
        if (!status.isPaid()) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Comprovante só existe para reserva paga");
        }
        return new Receipt(Receipt.TITLE, id, guestName, guestCpf, room.number(), room.type(), checkIn, checkOut,
                nights, subtotal, discount, total);
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
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
