package com.pousada.api.dto;

import com.pousada.domain.Reservation;

public class ReservationResponse {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String checkIn;
    public String checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public double subtotal;
    public double discount;
    public double total;
    public String status;
    public double amountPaid;
    public Double refund;

    public static ReservationResponse fromDomain(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn().toString();
        response.checkOut = reservation.getCheckOut().toString();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().getDisplayName();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().name();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();
        return response;
    }
}
