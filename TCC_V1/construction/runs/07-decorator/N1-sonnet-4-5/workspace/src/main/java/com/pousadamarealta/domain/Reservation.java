package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private Long id;
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

    public Reservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       BigDecimal nightlyRate) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;

        this.nights = (int) checkIn.datesUntil(checkOut).count();
        this.subtotal = calculateSubtotal(nightlyRate);
        this.discount = calculateDiscount();
        this.total = subtotal.subtract(discount);
    }

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

    private BigDecimal calculateSubtotal(BigDecimal nightlyRate) {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = nightlyRate;
            if (isHighSeason(current)) {
                nightPrice = nightPrice.multiply(new BigDecimal("1.20"));
            }
            nightPrice = round(nightPrice);
            sum = sum.add(nightPrice);
            current = current.plusDays(1);
        }
        return round(sum);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        return false;
    }

    private BigDecimal calculateDiscount() {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discountAmount = subtotal.multiply(ratePlan.getDiscountRate());
        return round(discountAmount);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED reservations can check in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only CHECKED_IN reservations can check out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only PENDING or CONFIRMED reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT;
    }

    public boolean occupiesRoomOn(LocalDate date) {
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        return !date.isBefore(checkIn) && date.isBefore(checkOut);
    }

    public BigDecimal getRevenue() {
        return amountPaid.subtract(refund != null ? refund : BigDecimal.ZERO);
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

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
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
