package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

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
    private final Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    private Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, Quote quote,
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
        this.quote = quote;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation pending(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      Quote quote) {
        return new Reservation(0, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public static Reservation restore(long id, int roomNumber, String guestName, String guestEmail,
                                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                                      RatePlan ratePlan, Quote quote, ReservationStatus status,
                                      BigDecimal amountPaid, BigDecimal refund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, status, amountPaid, refund);
    }

    public void confirm() {
        requireStatus("confirmar", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void registerCheckIn() {
        requireStatus("fazer o check-in de", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        requireStatus("fazer o check-out de", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public void requireReceipt() {
        if (!status.isPaidAndActive()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só reservas pagas têm comprovante; situação atual: " + status);
        }
    }

    /** Foi paga em algum momento (mesmo que depois tenha sido cancelada). */
    public boolean wasPaid() {
        return amountPaid.signum() > 0;
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Não é possível " + action + " uma reserva na situação " + status);
        }
    }

    public long id() { return id; }
    public void assignId(long id) { this.id = id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
