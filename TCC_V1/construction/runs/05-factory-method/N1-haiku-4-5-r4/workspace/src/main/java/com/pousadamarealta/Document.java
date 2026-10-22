package com.pousadamarealta;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    public final String checkIn;
    public final String checkOut;
    public final int nights;
    public final String subtotal;
    public final String discount;
    public final String total;
    public final String text;

    public Document(String title, int reservationId, String guestName,
                   String guestCpf, String companyCnpj, String companyName,
                   String guestPassport, String guestCountry,
                   int roomNumber, String roomType, String checkIn, String checkOut,
                   int nights, String subtotal, String discount, String total, String text) {
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
    }
}
