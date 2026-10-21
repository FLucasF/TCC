package com.pousada.api.dto;

import com.pousada.domain.Reservation;
import java.time.format.DateTimeFormatter;

public class ReservationResponse {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    public static ReservationResponse fromReservation(Reservation res) {
        ReservationResponse response = new ReservationResponse();
        response.id = res.getId();
        response.roomNumber = res.getRoomNumber();
        response.guestName = res.getGuestName();
        response.guestEmail = res.getGuestEmail();
        response.guestCpf = res.getGuestCpf();
        response.checkIn = res.getCheckIn().format(DATE_FORMAT);
        response.checkOut = res.getCheckOut().format(DATE_FORMAT);
        response.guests = res.getGuests();
        response.ratePlan = res.getRatePlan();
        response.nights = res.getNights();
        response.subtotal = res.getSubtotal();
        response.discount = res.getDiscount();
        response.total = res.getTotal();
        response.status = res.getStatus().toString();
        response.amountPaid = res.getAmountPaid();
        response.refund = res.getRefund();
        return response;
    }
}
