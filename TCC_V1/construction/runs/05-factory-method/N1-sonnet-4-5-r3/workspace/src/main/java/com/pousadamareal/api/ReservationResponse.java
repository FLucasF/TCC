package com.pousadamareal.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousadamareal.model.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    private final long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final String companyCnpj;
    private final String companyName;
    private final String guestPassport;
    private final String guestCountry;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate checkOut;

    private final int guests;
    private final String ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String status;
    private final BigDecimal amountPaid;
    private final BigDecimal refund;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.roomNumber = reservation.getRoomNumber();
        this.guestName = reservation.getGuestName();
        this.guestEmail = reservation.getGuestEmail();
        this.guestCpf = reservation.getIdentification().getGuestCpf();
        this.companyCnpj = reservation.getIdentification().getCompanyCnpj();
        this.companyName = reservation.getIdentification().getCompanyName();
        this.guestPassport = reservation.getIdentification().getGuestPassport();
        this.guestCountry = reservation.getIdentification().getGuestCountry();
        this.checkIn = reservation.getCheckIn();
        this.checkOut = reservation.getCheckOut();
        this.guests = reservation.getGuests();
        this.ratePlan = reservation.getRatePlan().name();
        this.nights = reservation.getNights();
        this.subtotal = reservation.getSubtotal();
        this.discount = reservation.getDiscount();
        this.total = reservation.getTotal();
        this.status = reservation.getStatus().name();
        this.amountPaid = reservation.getAmountPaid();
        this.refund = reservation.getRefund();
    }

    public long getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public String getCompanyCnpj() { return companyCnpj; }
    public String getCompanyName() { return companyName; }
    public String getGuestPassport() { return guestPassport; }
    public String getGuestCountry() { return guestCountry; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public String getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }
}
