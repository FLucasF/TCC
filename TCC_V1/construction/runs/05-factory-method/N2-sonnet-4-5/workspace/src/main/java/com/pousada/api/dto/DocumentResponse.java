package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousada.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DocumentResponse {
    private String title;
    private long reservationId;
    private String guestName;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;
    private String roomNumber;
    private String roomType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOut;

    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String text;

    public static DocumentResponse from(Reservation reservation, String documentText) {
        DocumentResponse response = new DocumentResponse();

        GuestIdentification identification = reservation.getIdentification();
        if (identification instanceof CpfIdentification) {
            response.title = "Comprovante de reserva";
        } else if (identification instanceof CnpjIdentification) {
            response.title = "Fatura de hospedagem";
        } else {
            response.title = "Booking receipt";
        }

        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();

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

        response.roomNumber = reservation.getRoomNumber();
        response.roomType = reservation.getRoomType().name();
        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.text = documentText;

        return response;
    }

    public String getTitle() {
        return title;
    }

    public long getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
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

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
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

    public String getText() {
        return text;
    }
}
