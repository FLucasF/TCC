package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    private final long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final BigDecimal nightlyRate;
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

    public Reservation(long id, String roomNumber, RoomType roomType, BigDecimal nightlyRate,
                       String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
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
    }

    public long id() {
        return id;
    }

    public String roomNumber() {
        return roomNumber;
    }

    public RoomType roomType() {
        return roomType;
    }

    public String guestName() {
        return guestName;
    }

    public String guestEmail() {
        return guestEmail;
    }

    public String guestCpf() {
        return guestCpf;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public int guests() {
        return guests;
    }

    public RatePlan ratePlan() {
        return ratePlan;
    }

    public ReservationStatus status() {
        return status;
    }

    public BigDecimal amountPaid() {
        return amountPaid;
    }

    public BigDecimal refund() {
        return refund;
    }

    public int nights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal subtotal() {
        List<LocalDate> nightDates = checkIn.datesUntil(checkOut).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate date : nightDates) {
            BigDecimal nightPrice = nightlyRate;
            if (isHighSeason(date)) {
                nightPrice = nightPrice.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(nightPrice);
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal discount() {
        if (ratePlan.discountRate().compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.00");
        }
        return subtotal().multiply(ratePlan.discountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal total() {
        return subtotal().subtract(discount())
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    public List<LocalDate> occupiedNights() {
        return new ArrayList<>(checkIn.datesUntil(checkOut).toList());
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total();
    }

    public void doCheckIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can check in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void doCheckOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can check out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel after check-in");
        }
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
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
}
