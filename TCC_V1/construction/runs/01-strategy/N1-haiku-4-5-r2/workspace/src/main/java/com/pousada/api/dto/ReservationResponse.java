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

    public static ReservationResponse from(Reservation reservation) {
        ReservationResponse dto = new ReservationResponse();
        dto.id = reservation.getId();
        dto.roomNumber = reservation.getRoomNumber();
        dto.guestName = reservation.getGuestName();
        dto.guestEmail = reservation.getGuestEmail();
        dto.guestCpf = reservation.getGuestCpf();
        dto.checkIn = formatDate(reservation.getCheckIn());
        dto.checkOut = formatDate(reservation.getCheckOut());
        dto.guests = reservation.getGuests();
        dto.ratePlan = reservation.getRatePlan();
        dto.nights = reservation.getNights();
        dto.subtotal = reservation.getSubtotal().doubleValue();
        dto.discount = reservation.getDiscount().doubleValue();
        dto.total = reservation.getTotal().doubleValue();
        dto.status = reservation.getStatus().toString();
        dto.amountPaid = reservation.getAmountPaid().doubleValue();
        dto.refund = reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null;
        return dto;
    }

    private static String formatDate(java.time.LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
