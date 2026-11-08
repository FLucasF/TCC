package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private long id;
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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
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
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    /** Reserva nova, pendente, ainda sem número. */
    public static Reservation create(Room room, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (guests < 1) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (guests > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        BigDecimal subtotal = Pricing.subtotal(room, checkIn, checkOut);
        BigDecimal discount = ratePlan.discount(subtotal);
        return new Reservation(0, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                subtotal, discount, Money.round(subtotal.subtract(discount)),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        requireStatus("confirmar", ReservationStatus.PENDING);
        amountPaid = total;
        status = ReservationStatus.CONFIRMED;
    }

    public void startStay() {
        requireStatus("fazer o check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        requireStatus("fazer o check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    private void requireStatus(String operation, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + operation + " uma reserva com situação " + status);
    }

    public Receipt receipt() {
        if (!status.isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe comprovante para reserva paga; situação atual: " + status);
        }
        return new Receipt(id, guestName, guestCpf, room.number(), room.type(), checkIn, checkOut,
                nights(), subtotal, discount, total);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public void assignId(long id) {
        this.id = id;
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
