package com.pousada.domain;

import com.pousada.domain.rateplan.CorporateRatePlan;
import com.pousada.domain.rateplan.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlanType ratePlanType;
    private String companyCode;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation() {
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
    }

    public void calculatePricing(BigDecimal nightlyRate) {
        RatePlan ratePlan = ratePlanType.create();

        this.nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = ratePlan.calculateSubtotal(nightlyRate, checkIn, checkOut);

        if (ratePlanType == RatePlanType.CORPORATE && companyCode != null) {
            this.discount = ((CorporateRatePlan) ratePlan).calculateDiscount(subtotal, companyCode);
        } else {
            this.discount = ratePlan.calculateDiscount(subtotal, nightlyRate, checkIn, checkOut);
        }

        this.total = subtotal.subtract(discount);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Apenas reservas pendentes podem ser confirmadas");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Apenas reservas confirmadas podem fazer check-in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Apenas reservas com check-in podem fazer check-out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Apenas reservas pendentes ou confirmadas podem ser canceladas");
        }

        RatePlan ratePlan = ratePlanType.create();
        this.refund = ratePlan.calculateRefund(amountPaid);
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean isActive() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public BigDecimal getNetRevenue() {
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

    public RatePlanType getRatePlanType() {
        return ratePlanType;
    }

    public void setRatePlanType(RatePlanType ratePlanType) {
        this.ratePlanType = ratePlanType;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
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
}
