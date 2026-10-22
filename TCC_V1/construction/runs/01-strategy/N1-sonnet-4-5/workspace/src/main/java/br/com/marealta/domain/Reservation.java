package br.com.marealta.domain;

import br.com.marealta.domain.rateplan.CorporateRatePlan;
import br.com.marealta.domain.rateplan.RatePlan;
import br.com.marealta.domain.rateplan.RatePlanFactory;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final DateRange dateRange;
    private final int guests;
    private final String ratePlanType;
    private final String companyCode;
    private ReservationStatus status;
    private Money amountPaid;
    private Money refund;

    public Reservation(Long id, String roomNumber, RoomType roomType, String guestName,
                      String guestEmail, String guestCpf, DateRange dateRange,
                      int guests, String ratePlanType, String companyCode) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.dateRange = dateRange;
        this.guests = guests;
        this.ratePlanType = ratePlanType;
        this.companyCode = companyCode;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = Money.zero();
        this.refund = null;
    }

    public void validate() throws CapacityValidationException {
        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }
        if (guests > roomType.getCapacity()) {
            throw new CapacityValidationException("Number of guests exceeds room capacity");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        ratePlan.validate(roomType, dateRange, guests, companyCode);
    }

    public static class CapacityValidationException extends RuntimeException {
        public CapacityValidationException(String message) {
            super(message);
        }
    }

    public void confirm() {
        if (!status.canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status: " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = calculateTotal();
    }

    public void cancel() {
        if (!status.canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status: " + status);
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        this.refund = ratePlan.calculateRefund(amountPaid);
        this.status = ReservationStatus.CANCELLED;
    }

    public void checkIn() {
        if (!status.canCheckIn()) {
            throw new IllegalStateException("Cannot check-in reservation in status: " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (!status.canCheckOut()) {
            throw new IllegalStateException("Cannot check-out reservation in status: " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public Money calculateSubtotal() {
        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        return ratePlan.calculateSubtotal(roomType, dateRange);
    }

    public Money calculateDiscount() {
        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        Money subtotal = calculateSubtotal();

        if (ratePlan instanceof CorporateRatePlan corporate) {
            return corporate.calculateDiscount(subtotal, companyCode);
        }

        return ratePlan.calculateDiscount(subtotal);
    }

    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        Money discount = calculateDiscount();
        return subtotal.subtract(discount);
    }

    public Money getRevenue() {
        return amountPaid.subtract(refund != null ? refund : Money.zero());
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

    public DateRange getDateRange() {
        return dateRange;
    }

    public int getGuests() {
        return guests;
    }

    public String getRatePlanType() {
        return ratePlanType;
    }

    public String getCompanyCode() {
        return companyCode;
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
}
