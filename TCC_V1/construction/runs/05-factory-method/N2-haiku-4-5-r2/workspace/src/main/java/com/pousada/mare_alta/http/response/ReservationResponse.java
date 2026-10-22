package com.pousada.mare_alta.http.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousada.mare_alta.model.Reservation;
import com.pousada.mare_alta.util.NumberFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationResponse {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String companyCnpj;
    public String companyName;
    public String guestPassport;
    public String guestCountry;
    public String checkIn;
    public String checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public String subtotal;
    public String discount;
    public String total;
    public String status;
    public String amountPaid;
    public String refund;

    public static ReservationResponse fromModel(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.companyCnpj = reservation.getCompanyCnpj();
        response.companyName = reservation.getCompanyName();
        response.guestPassport = reservation.getGuestPassport();
        response.guestCountry = reservation.getGuestCountry();
        response.checkIn = reservation.getCheckIn().toString();
        response.checkOut = reservation.getCheckOut().toString();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().getValue();
        response.nights = reservation.getNights();
        response.subtotal = NumberFormatter.formatCurrencyForJson(reservation.getSubtotal());
        response.discount = NumberFormatter.formatCurrencyForJson(reservation.getDiscount());
        response.total = NumberFormatter.formatCurrencyForJson(reservation.getTotal());
        response.status = reservation.getStatus().getValue();
        response.amountPaid = NumberFormatter.formatCurrencyForJson(reservation.getAmountPaid());
        response.refund = reservation.getRefund() != null ?
                NumberFormatter.formatCurrencyForJson(reservation.getRefund()) : null;
        return response;
    }
}
