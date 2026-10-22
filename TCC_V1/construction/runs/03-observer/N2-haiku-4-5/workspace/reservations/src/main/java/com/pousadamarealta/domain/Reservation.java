package com.pousadamarealta.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private long id;
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private ReservationStatus status;
    private Money amountPaid;
    private Money refund;
    private int nights;
    private Money subtotal;
    private Money discount;
    private Money total;

    public Reservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                      double nightlyRate) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = Money.zero();
        this.refund = null;

        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        calculatePricing(nightlyRate);
    }

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                      ReservationStatus status, int nights, Money subtotal, Money discount, Money total,
                      Money amountPaid, Money refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = status;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    private void calculatePricing(double baseNightlyRate) {
        Money subtotalAmount = Money.zero();
        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            Money nightRate = SeasonalityCalculator.calculateNightlyRate(baseNightlyRate, night);
            subtotalAmount = subtotalAmount.add(nightRate);
        }
        this.subtotal = subtotalAmount;
        this.discount = calculateDiscount();
        this.total = subtotal.subtract(discount);
    }

    private Money calculateDiscount() {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return Money.of(subtotal.toDouble() * 0.15);
        }
        return Money.zero();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Money getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Money amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Money getRefund() {
        return refund;
    }

    public void setRefund(Money refund) {
        this.refund = refund;
    }

    public int getNights() {
        return nights;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getDiscount() {
        return discount;
    }

    public Money getTotal() {
        return total;
    }
}
