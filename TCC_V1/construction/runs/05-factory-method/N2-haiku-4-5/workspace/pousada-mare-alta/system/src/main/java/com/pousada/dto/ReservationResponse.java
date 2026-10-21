package com.pousada.dto;

import com.google.gson.annotations.SerializedName;
import com.pousada.domain.Reservation;

public class ReservationResponse {
    @SerializedName("id")
    public int id;

    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestEmail")
    public String guestEmail;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("companyCnpj")
    public String companyCnpj;

    @SerializedName("companyName")
    public String companyName;

    @SerializedName("guestPassport")
    public String guestPassport;

    @SerializedName("guestCountry")
    public String guestCountry;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    @SerializedName("guests")
    public int guests;

    @SerializedName("ratePlan")
    public String ratePlan;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount")
    public double discount;

    @SerializedName("total")
    public double total;

    @SerializedName("status")
    public String status;

    @SerializedName("amountPaid")
    public double amountPaid;

    @SerializedName("refund")
    public Double refund;

    public static ReservationResponse fromReservation(Reservation res) {
        ReservationResponse response = new ReservationResponse();
        response.id = res.getId();
        response.roomNumber = res.getRoomNumber();
        response.guestName = res.getGuestName();
        response.guestEmail = res.getGuestEmail();
        response.guestCpf = res.getGuestCpf();
        response.companyCnpj = res.getCompanyCnpj();
        response.companyName = res.getCompanyName();
        response.guestPassport = res.getGuestPassport();
        response.guestCountry = res.getGuestCountry();
        response.checkIn = res.getCheckIn().toString();
        response.checkOut = res.getCheckOut().toString();
        response.guests = res.getGuests();
        response.ratePlan = res.getRatePlan().name();
        response.nights = res.getNights();
        response.subtotal = res.getSubtotal();
        response.discount = res.getDiscount();
        response.total = res.getTotal();
        response.status = res.getStatus().name();
        response.amountPaid = res.getAmountPaid();
        response.refund = res.getRefund();
        return response;
    }
}
