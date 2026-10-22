package br.com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final RoomType roomType;
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

    public Reservation(String roomNumber, RoomType roomType, String guestName, String guestEmail,
                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) checkIn.datesUntil(checkOut).count();
        this.subtotal = calculateSubtotal();
        this.discount = calculateDiscount();
        this.total = subtotal.subtract(discount);
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
    }

    private BigDecimal calculateSubtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            BigDecimal nightRate = roomType.getNightlyRate();
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            sum = sum.add(nightRate);
            date = date.plusDays(1);
        }
        return sum.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) return true;
        if (month == Month.JANUARY || month == Month.FEBRUARY) return true;
        if (month == Month.JULY) return true;

        return false;
    }

    private BigDecimal calculateDiscount() {
        BigDecimal discountAmount = subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        return discountAmount;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
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

        boolean wasConfirmed = status == ReservationStatus.CONFIRMED;
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = this.amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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
