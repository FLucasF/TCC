package com.pousadamarealta;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reservation {
    public final int id;
    public final int roomNumber;
    public final String guestName;
    public final String guestEmail;
    public final String guestCpf;
    public final String companyCnpj;
    public final String companyName;
    public final String guestPassport;
    public final String guestCountry;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int guests;
    public final RatePlan ratePlan;
    public final int nights;
    public final double subtotal;
    public final double discount;
    public final double total;
    public final ReservationStatus status;
    public final double amountPaid;
    public final Double refund;

    public Reservation(int id, int roomNumber, String guestName, String guestEmail,
                      String guestCpf, String companyCnpj, String companyName,
                      String guestPassport, String guestCountry,
                      LocalDate checkIn, LocalDate checkOut, int guests,
                      RatePlan ratePlan, int nights, double subtotal, double discount,
                      double total, ReservationStatus status, double amountPaid, Double refund) {
        this.id = id;
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
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }
}
