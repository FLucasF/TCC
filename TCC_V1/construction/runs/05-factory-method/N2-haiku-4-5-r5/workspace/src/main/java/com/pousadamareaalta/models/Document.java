package com.pousadamareaalta.models;

public class Document {
    public String title;
    public Long reservationId;
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
    public double subtotal;
    public double discount;
    public double total;
    public String text;

    public Document() {}

    public Document(Long reservationId, String guestName, String guestCpf,
                   String companyCnpj, String companyName,
                   String guestPassport, String guestCountry,
                   int roomNumber, String roomType,
                   String checkIn, String checkOut, int nights,
                   double subtotal, double discount, double total, String text) {
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
}
