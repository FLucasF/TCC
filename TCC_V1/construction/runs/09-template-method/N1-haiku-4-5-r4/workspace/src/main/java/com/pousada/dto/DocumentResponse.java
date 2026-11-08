package com.pousada.dto;

import java.math.BigDecimal;

public class DocumentResponse {
    public String title = "Comprovante de reserva";
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
}
