package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousada.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    private long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOut;

    private int guests;
    private String ratePlan;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();

        GuestIdentification identification = reservation.getIdentification();
        if (identification instanceof CpfIdentification cpf) {
            response.guestCpf = cpf.cpf();
            response.companyCnpj = null;
            response.companyName = null;
            response.guestPassport = null;
            response.guestCountry = null;
        } else if (identification instanceof CnpjIdentification cnpj) {
            response.guestCpf = null;
            response.companyCnpj = cnpj.cnpj();
            response.companyName = cnpj.companyName();
            response.guestPassport = null;
            response.guestCountry = null;
        } else if (identification instanceof PassportIdentification passport) {
            response.guestCpf = null;
            response.companyCnpj = null;
            response.companyName = null;
            response.guestPassport = passport.passport();
            response.guestCountry = passport.country();
        }

        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().name();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().name();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();

        return response;
    }

    public long getId() {
        return id;
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

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
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

    public String getRatePlan() {
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

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
