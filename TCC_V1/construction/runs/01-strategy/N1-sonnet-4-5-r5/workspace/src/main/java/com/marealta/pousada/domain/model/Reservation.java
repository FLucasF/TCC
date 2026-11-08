package com.marealta.pousada.domain.model;

import com.marealta.pousada.domain.rateplan.RatePlan;
import com.marealta.pousada.domain.rateplan.RatePlanFactory;
import com.marealta.pousada.exception.OperationNotAllowedException;
import com.marealta.pousada.util.DateUtil;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private Long id;
    private final int roomNumber;
    private final RoomType roomType;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String ratePlanName;
    private final long nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, RoomType roomType, String guestName, String guestEmail,
                      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                      String ratePlanName, String companyCode) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlanName = ratePlanName;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;

        RatePlan ratePlan = RatePlanFactory.create(ratePlanName);
        ratePlan.validate(checkIn, checkOut, roomType, companyCode);

        this.nights = DateUtil.countNights(checkIn, checkOut);
        this.subtotal = ratePlan.calculateSubtotal(checkIn, checkOut, roomType);
        this.discount = ratePlan.calculateDiscount(subtotal);
        this.total = MoneyUtil.round(subtotal.subtract(discount));
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm pending reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check in confirmed reservations");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check out reservations with check-in done");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Cannot cancel reservation after check-in");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanName);
        this.refund = ratePlan.calculateRefund(amountPaid);
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getRevenueAmount() {
        return MoneyUtil.round(amountPaid.subtract(refund != null ? refund : BigDecimal.ZERO));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRoomNumber() {
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

    public String getRatePlanName() {
        return ratePlanName;
    }

    public long getNights() {
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
