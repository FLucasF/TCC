package com.pousadamareaalta.models;

import java.time.LocalDate;

public class Reservation {
    public Long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String companyCnpj;
    public String companyName;
    public String guestPassport;
    public String guestCountry;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public double subtotal;
    public double discount;
    public double total;
    public String status;
    public double amountPaid;
    public Double refund;

    public Reservation() {}

    public Reservation(
            int roomNumber, String guestName, String guestEmail,
            String guestCpf, String companyCnpj, String companyName,
            String guestPassport, String guestCountry,
            LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
            int nights, double subtotal, double discount, double total
    ) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = "PENDING";
        this.amountPaid = 0;
        this.refund = null;
    }
}
