package com.pousadamareaalta.dto;

import com.pousadamareaalta.model.Reservation;

public class ReservationDTO {
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

    public static ReservationDTO fromReservation(Reservation res) {
        ReservationDTO dto = new ReservationDTO();
        dto.id = res.getId();
        dto.roomNumber = res.getRoomNumber();
        dto.guestName = res.getGuestName();
        dto.guestEmail = res.getGuestEmail();
        dto.guestCpf = res.getGuestCpf();
        dto.checkIn = res.getCheckIn().toString();
        dto.checkOut = res.getCheckOut().toString();
        dto.guests = res.getGuests();
        dto.ratePlan = res.getRatePlan().toString();
        dto.nights = res.getNights();
        dto.subtotal = res.getSubtotal().doubleValue();
        dto.discount = res.getDiscount().doubleValue();
        dto.total = res.getTotal().doubleValue();
        dto.status = res.getStatus().toString();
        dto.amountPaid = res.getAmountPaid().doubleValue();
        dto.refund = res.getRefund() != null ? res.getRefund().doubleValue() : null;
        return dto;
    }
}
