package com.mareaalta.domain;

import java.math.BigDecimal;

public class DocumentResponse {
    private String title;
    private long reservationId;
    private String guestName;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;
    private int roomNumber;
    private String roomType;
    private String checkIn;
    private String checkOut;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String text;

    public DocumentResponse(String title, long reservationId, String guestName,
                           String guestCpf, String companyCnpj, String companyName,
                           String guestPassport, String guestCountry,
                           int roomNumber, String roomType, String checkIn, String checkOut,
                           int nights, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal total, String text) {
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

    public String getTitle() { return title; }
    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public String getGuestCpf() { return guestCpf; }
    public String getCompanyCnpj() { return companyCnpj; }
    public String getCompanyName() { return companyName; }
    public String getGuestPassport() { return guestPassport; }
    public String getGuestCountry() { return guestCountry; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getText() { return text; }
}
