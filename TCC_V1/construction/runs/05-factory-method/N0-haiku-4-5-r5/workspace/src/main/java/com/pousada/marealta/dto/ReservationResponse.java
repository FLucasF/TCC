package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.marealta.model.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String companyCnpj;
    public String companyName;
    public String guestPassport;
    public String guestCountry;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public static ReservationResponse from(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.id = r.getId();
        resp.roomNumber = r.getRoomNumber();
        resp.guestName = r.getGuestName();
        resp.guestEmail = r.getGuestEmail();
        resp.guestCpf = r.getGuestCpf();
        resp.companyCnpj = r.getCompanyCnpj();
        resp.companyName = r.getCompanyName();
        resp.guestPassport = r.getGuestPassport();
        resp.guestCountry = r.getGuestCountry();
        resp.checkIn = r.getCheckIn();
        resp.checkOut = r.getCheckOut();
        resp.guests = r.getGuests();
        resp.ratePlan = r.getRatePlan().name();
        resp.nights = r.getNights();
        resp.subtotal = r.getSubtotal();
        resp.discount = r.getDiscount();
        resp.total = r.getTotal();
        resp.status = r.getStatus().name();
        resp.amountPaid = r.getAmountPaid();
        resp.refund = r.getRefund();
        return resp;
    }
}
