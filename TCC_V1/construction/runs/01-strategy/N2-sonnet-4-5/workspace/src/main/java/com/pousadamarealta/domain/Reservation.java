package com.pousadamarealta.domain;

import com.pousadamarealta.domain.rateplan.RatePlan;
import com.pousadamarealta.domain.rateplan.RatePlanFactory;
import com.pousadamarealta.exception.OperationNotAllowedException;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final RatePlanType ratePlanType;
    private final long nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, RoomType roomType, String guestName,
                      String guestEmail, String guestCpf, LocalDate checkIn, LocalDate checkOut,
                      int guests, RatePlanType ratePlanType, long nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total, ReservationStatus status,
                      BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlanType = ratePlanType;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation create(String roomNumber, Room room, String guestName,
                                    String guestEmail, String guestCpf, LocalDate checkIn,
                                    LocalDate checkOut, int guests, RatePlanType ratePlanType,
                                    String companyCode) {
        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        ratePlan.validate(checkIn, checkOut, companyCode);

        long nights = PriceCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = ratePlan.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = ratePlan.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        return new Reservation(
            null,
            roomNumber,
            room.getType(),
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlanType,
            nights,
            subtotal,
            discount,
            total,
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );
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
            throw new OperationNotAllowedException("Can only check out after check-in");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only cancel pending or confirmed reservations");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        this.refund = ratePlan.calculateRefund(amountPaid);
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getNetRevenue() {
        return amountPaid.subtract(refund != null ? refund : BigDecimal.ZERO);
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

    public RatePlanType getRatePlanType() {
        return ratePlanType;
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
