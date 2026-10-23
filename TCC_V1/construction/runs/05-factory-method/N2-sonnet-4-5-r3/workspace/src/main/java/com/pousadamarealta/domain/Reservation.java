package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class Reservation {
    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;
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

    public int getNights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal getSubtotal(Room room) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            total = total.add(nightRate);
            date = date.plusDays(1);
        }

        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getDiscount(BigDecimal subtotal) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate())
            .setScale(2, RoundingMode.HALF_EVEN);
        return discount;
    }

    public BigDecimal getTotal(Room room) {
        BigDecimal subtotal = getSubtotal(room);
        BigDecimal discount = getDiscount(subtotal);
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
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

    public boolean canConfirm() {
        return status == ReservationStatus.PENDING;
    }

    public boolean canCheckIn() {
        return status == ReservationStatus.CONFIRMED;
    }

    public boolean canCheckOut() {
        return status == ReservationStatus.CHECKED_IN;
    }

    public boolean canCancel() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    public void confirm(BigDecimal amount) {
        if (!canConfirm()) {
            throw new IllegalStateException("Cannot confirm reservation in status " + status);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = amount;
    }

    public void checkIn() {
        if (!canCheckIn()) {
            throw new IllegalStateException("Cannot check-in reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (!canCheckOut()) {
            throw new IllegalStateException("Cannot check-out reservation in status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel reservation in status " + status);
        }
        this.status = ReservationStatus.CANCELLED;

        if (ratePlan == RatePlan.STANDARD) {
            this.refund = this.amountPaid;
        } else {
            this.refund = BigDecimal.ZERO;
        }
    }

    public boolean isActive() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public boolean hasIndividualGuest() {
        return guestCpf != null;
    }

    public boolean hasCorporateGuest() {
        return companyCnpj != null;
    }

    public boolean hasInternationalGuest() {
        return guestPassport != null;
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

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public void setCompanyCnpj(String companyCnpj) {
        this.companyCnpj = companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public void setGuestPassport(String guestPassport) {
        this.guestPassport = guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public void setGuestCountry(String guestCountry) {
        this.guestCountry = guestCountry;
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
}
