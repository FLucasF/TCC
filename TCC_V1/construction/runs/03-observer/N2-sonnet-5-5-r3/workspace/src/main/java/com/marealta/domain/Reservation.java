package com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {
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

    public static Reservation open(Room room, String guestName, String guestEmail, String guestCpf,
                                   LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        return new Reservation(0, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, Quote.of(room, checkIn, checkOut, ratePlan), ReservationStatus.PENDING, Money.ZERO, null);
    }

    public static Reservation restore(long id, int roomNumber, String guestName, String guestEmail,
                                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                                      RatePlan ratePlan, Quote quote, ReservationStatus status,
                                      BigDecimal amountPaid, BigDecimal refund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, status, amountPaid, refund);
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar uma reserva pendente ou confirmada");
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public void registerCheckIn() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    private static void require(boolean allowed, String message) {
        if (!allowed) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED, message);
        }
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
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    /** Nulo enquanto a reserva não for cancelada. */
    public BigDecimal refund() { return refund; }

    /** O que ficou com a pousada: o pago menos o devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.of(amountPaid.subtract(refund));
    }
}
