package br.com.mare.domain;

import br.com.mare.plan.RatePlan;

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
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private Status status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(int id, Room room, String guestName, String guestEmail, String guestCpf, LocalDate checkIn,
                       LocalDate checkOut, int guests, RatePlan ratePlan, int nights, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total, Status status, BigDecimal amountPaid, BigDecimal refund) {
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

    public void confirm() {
        requireStatus("confirmar", Status.PENDING);
        status = Status.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        requireStatus("fazer o check-in de", Status.CONFIRMED);
        status = Status.CHECKED_IN;
    }

    public void checkOut() {
        requireStatus("fazer o check-out de", Status.CHECKED_IN);
        status = Status.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelar", Status.PENDING, Status.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
        status = Status.CANCELLED;
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw ApiException.notAllowed("Só existe comprovante para reserva paga; situação atual: " + status);
        }
    }

    private void requireStatus(String action, Status... allowed) {
        for (Status s : allowed) {
            if (s == status) return;
        }
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva " + status);
    }

    public int id() { return id; }
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
    public Status status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }

    /** Dinheiro que fica com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal netRevenue() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }
}
