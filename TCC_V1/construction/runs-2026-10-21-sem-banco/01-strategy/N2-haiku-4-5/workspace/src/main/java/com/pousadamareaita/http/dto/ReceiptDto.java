package com.pousadamareaita.http.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ReceiptDto {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String title;
    public int reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public double subtotal;
    public double discount;
    public double total;

    public ReceiptDto(com.pousadamareaita.model.Reservation res, String roomType) {
        this.title = "Comprovante de reserva";
        this.reservationId = res.getId();
        this.guestName = res.getGuestName();
        this.guestCpf = res.getGuestCpf();
        this.roomNumber = res.getRoomNumber();
        this.roomType = roomType;
        this.checkIn = res.getCheckIn().format(DATE_FORMATTER);
        this.checkOut = res.getCheckOut().format(DATE_FORMATTER);
        this.nights = res.getNights();
        this.subtotal = res.getSubtotal().doubleValue();
        this.discount = res.getDiscount().doubleValue();
        this.total = res.getTotal().doubleValue();
    }
}
