package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final BigDecimal baseNightlyRate;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, RoomType roomType, BigDecimal baseNightlyRate,
                       String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.baseNightlyRate = baseNightlyRate;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = status;
        this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        this.refund = refund;
    }

    public static Reservation createNew(String roomNumber, RoomType roomType, BigDecimal baseNightlyRate,
                                        String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        return new Reservation(null, roomNumber, roomType, baseNightlyRate,
                guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                ReservationStatus.PENDING, BigDecimal.ZERO, null);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = calculateTotal();
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        BigDecimal refundAmount = calculateRefund();
        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }

    private BigDecimal calculateRefund() {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }
        return amountPaid;
    }

    public int getNights() {
        return (int) checkIn.until(checkOut).getDays();
    }

    public BigDecimal calculateSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = calculateNightRate(current);
            total = total.add(nightRate);
            current = current.plusDays(1);
        }

        return roundToEven(total);
    }

    private BigDecimal calculateNightRate(LocalDate date) {
        BigDecimal rate = baseNightlyRate;
        if (isHighSeason(date)) {
            rate = rate.multiply(new BigDecimal("1.20"));
        }
        return roundToEven(rate);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateDiscount() {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = calculateSubtotal();
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return roundToEven(discount);
    }

    public BigDecimal calculateTotal() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal discount = calculateDiscount();
        return roundToEven(subtotal.subtract(discount));
    }

    private BigDecimal roundToEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
            || status == ReservationStatus.CHECKED_IN
            || status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getNetRevenue() {
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal refunded = refund != null ? refund : BigDecimal.ZERO;
        return paid.subtract(refunded);
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

    public RoomType getRoomType() {
        return roomType;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
