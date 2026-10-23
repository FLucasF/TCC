package com.pousadamarealta.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation() {
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(RatePlan ratePlan) {
        this.ratePlan = ratePlan;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public void setRefund(BigDecimal refund) {
        this.refund = refund;
    }

    public int getNights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightRate = baseRate;
            if (isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(ratePlan.getDiscountRate())
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
