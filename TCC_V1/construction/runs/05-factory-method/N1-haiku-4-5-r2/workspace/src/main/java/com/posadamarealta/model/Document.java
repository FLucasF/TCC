package com.posadamarealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Document {
    public String title;
    public int reservationId;
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

    public Document() {}
}
