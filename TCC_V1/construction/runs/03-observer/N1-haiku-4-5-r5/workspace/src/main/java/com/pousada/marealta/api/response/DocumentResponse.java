package com.pousada.marealta.api.response;

import com.pousada.marealta.model.Document;

public class DocumentResponse {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public String subtotal;
    public String discount;
    public String total;

    public DocumentResponse(Document document) {
        this.title = document.getTitle();
        this.reservationId = document.getReservationId();
        this.guestName = document.getGuestName();
        this.guestCpf = document.getGuestCpf();
        this.roomNumber = document.getRoomNumber();
        this.roomType = document.getRoomType();
        this.checkIn = document.getCheckIn().toString();
        this.checkOut = document.getCheckOut().toString();
        this.nights = document.getNights();
        this.subtotal = String.format("%.2f", document.getSubtotal());
        this.discount = String.format("%.2f", document.getDiscount());
        this.total = String.format("%.2f", document.getTotal());
    }
}
