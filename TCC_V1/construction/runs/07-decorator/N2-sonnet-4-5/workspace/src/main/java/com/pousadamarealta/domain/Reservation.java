package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Reservation {
    private final Long id;
    private final String roomNumber;
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

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
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

    public static Reservation create(String roomNumber, RoomType roomType, String guestName, String guestEmail,
                                    String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                                    RatePlan ratePlan, PriceCalculator calculator) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = calculator.calculateSubtotal(roomType, checkIn, checkOut);
        BigDecimal discount = calculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = calculator.calculateTotal(subtotal, discount);

        return new Reservation(null, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests, ratePlan, nights, subtotal, discount, total, ReservationStatus.PENDING,
                BigDecimal.ZERO, null);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void performCheckIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void performCheckOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = this.amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean occupiesRoom(LocalDate date) {
        return (date.isEqual(checkIn) || date.isAfter(checkIn)) && date.isBefore(checkOut);
    }

    public Long id() { return id; }
    public String roomNumber() { return roomNumber; }
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
