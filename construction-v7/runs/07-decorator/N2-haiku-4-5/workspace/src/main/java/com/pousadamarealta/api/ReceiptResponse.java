package com.pousadamarealta.api;

import java.math.BigDecimal;

public class ReceiptResponse {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public long nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;

    public ReceiptResponse(
        String title, long reservationId, String guestName, String guestCpf,
        int roomNumber, String roomType, String checkIn, String checkOut, long nights,
        BigDecimal subtotal, BigDecimal discount, BigDecimal total
    ) {
        this.title = title;
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
