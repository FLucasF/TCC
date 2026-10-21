package com.pousadamarealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Document {
    public final String title;
    public final int reservationId;
    public final String guestName;
    public final String guestCpf;
    public final String companyCnpj;
    public final String companyName;
    public final String guestPassport;
    public final String guestCountry;
    public final int roomNumber;
    public final String roomType;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;
    public final String text;
    public final BigDecimal issRetained;

    public Document(String title, int reservationId, String guestName, String guestCpf,
                   String companyCnpj, String companyName, String guestPassport, String guestCountry,
                   int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                   int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                   String text, BigDecimal issRetained) {
        this.title = title;
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.text = text;
        this.issRetained = issRetained;
    }
}
