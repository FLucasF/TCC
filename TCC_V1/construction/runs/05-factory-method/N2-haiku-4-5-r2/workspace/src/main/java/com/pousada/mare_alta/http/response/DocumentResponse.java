package com.pousada.mare_alta.http.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousada.mare_alta.model.Document;
import com.pousada.mare_alta.util.NumberFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public String companyCnpj;
    public String companyName;
    public String guestPassport;
    public String guestCountry;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public String subtotal;
    public String discount;
    public String total;
    public String text;

    public static DocumentResponse fromModel(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.title = document.getTitle();
        response.reservationId = document.getReservationId();
        response.guestName = document.getGuestName();
        response.guestCpf = document.getGuestCpf();
        response.companyCnpj = document.getCompanyCnpj();
        response.companyName = document.getCompanyName();
        response.guestPassport = document.getGuestPassport();
        response.guestCountry = document.getGuestCountry();
        response.roomNumber = document.getRoomNumber();
        response.roomType = document.getRoomType();
        response.checkIn = document.getCheckIn().toString();
        response.checkOut = document.getCheckOut().toString();
        response.nights = document.getNights();
        response.subtotal = NumberFormatter.formatCurrencyForJson(document.getSubtotal());
        response.discount = NumberFormatter.formatCurrencyForJson(document.getDiscount());
        response.total = NumberFormatter.formatCurrencyForJson(document.getTotal());
        response.text = document.getText();
        return response;
    }
}
