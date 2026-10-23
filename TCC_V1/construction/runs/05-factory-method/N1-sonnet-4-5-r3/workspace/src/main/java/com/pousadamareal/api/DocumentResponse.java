package com.pousadamareal.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousadamareal.model.Document;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DocumentResponse {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final String companyCnpj;
    private final String companyName;
    private final String guestPassport;
    private final String guestCountry;
    private final String roomNumber;
    private final String roomType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate checkOut;

    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String text;

    public DocumentResponse(Document document) {
        this.title = document.getTitle();
        this.reservationId = document.getReservationId();
        this.guestName = document.getGuestName();
        this.guestCpf = document.getGuestCpf();
        this.companyCnpj = document.getCompanyCnpj();
        this.companyName = document.getCompanyName();
        this.guestPassport = document.getGuestPassport();
        this.guestCountry = document.getGuestCountry();
        this.roomNumber = document.getRoomNumber();
        this.roomType = document.getRoomType().name();
        this.checkIn = document.getCheckIn();
        this.checkOut = document.getCheckOut();
        this.nights = document.getNights();
        this.subtotal = document.getSubtotal();
        this.discount = document.getDiscount();
        this.total = document.getTotal();
        this.text = document.getText();
    }

    public String getTitle() { return title; }
    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public String getGuestCpf() { return guestCpf; }
    public String getCompanyCnpj() { return companyCnpj; }
    public String getCompanyName() { return companyName; }
    public String getGuestPassport() { return guestPassport; }
    public String getGuestCountry() { return guestCountry; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getText() { return text; }
}
