package com.pousada.api.dto;

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
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;

    public ReceiptResponse() {}
}
