package com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static com.marealta.pousada.domain.ReservationStatus.*;

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

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
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

    public static Reservation open(NewReservation request, Quote quote) {
        return new Reservation(0, request.roomNumber(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                quote, PENDING, Money.ZERO, null);
    }

    public void confirm() {
        require(PENDING);
        status = CONFIRMED;
        amountPaid = quote.total();
    }

    public void registerCheckIn() {
        require(CONFIRMED);
        status = CHECKED_IN;
    }

    public void registerCheckOut() {
        require(CHECKED_IN);
        status = CHECKED_OUT;
    }

    public void cancel() {
        require(PENDING, CONFIRMED);
        status = CANCELLED;
        refund = ratePlan.refundOf(amountPaid);
    }

    private void require(ReservationStatus... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operação não permitida para reserva com status " + status);
        }
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal kept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupiesNight(LocalDate night) {
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
    public Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
