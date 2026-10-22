package com.pousada.marealta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String text;
}
