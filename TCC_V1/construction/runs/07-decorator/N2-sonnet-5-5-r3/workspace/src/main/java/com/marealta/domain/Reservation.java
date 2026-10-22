package com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {
    private Long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final Pricing.Quote quote;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       Pricing.Quote quote, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
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
        if (guests > room.capacity()) {
            throw new HotelException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote, ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = quote.total();
    }

    public void startStay() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void endStay() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    /** What the guesthouse keeps: paid minus refunded. */
    public BigDecimal retained() {
        return Money.round(amountPaid.subtract(refund == null ? Money.ZERO : refund));
    }

    public Receipt receipt() {
        require(status.isPaid(), "O comprovante só existe para reserva paga");
        return new Receipt(id, guestName, guestCpf, room.number(), room.type(), checkIn, checkOut,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total());
    }

    private void require(boolean allowed, String message) {
        if (!allowed) {
            throw new HotelException(ErrorCode.OPERATION_NOT_ALLOWED, message);
        }
    }

    public Long id() { return id; }
    public void assignId(long id) { this.id = id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public Pricing.Quote quote() { return quote; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
