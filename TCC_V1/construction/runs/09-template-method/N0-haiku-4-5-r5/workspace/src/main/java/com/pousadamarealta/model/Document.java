package com.pousadamarealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Document {
    public String title;
    public int reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;

    public Document(int reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                   LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
                   BigDecimal discount, BigDecimal total) {
        this.title = "Comprovante de reserva";
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }
}
