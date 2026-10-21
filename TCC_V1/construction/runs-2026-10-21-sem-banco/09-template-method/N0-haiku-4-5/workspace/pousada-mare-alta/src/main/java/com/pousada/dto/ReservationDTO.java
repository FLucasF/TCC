package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pousada.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDTO {
    public long id;
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public static ReservationDTO from(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.id = reservation.getId();
        dto.roomNumber = reservation.getRoomNumber();
        dto.guestName = reservation.getGuestName();
        dto.guestEmail = reservation.getGuestEmail();
        dto.guestCpf = reservation.getGuestCpf();
        dto.checkIn = reservation.getCheckIn();
        dto.checkOut = reservation.getCheckOut();
        dto.guests = reservation.getGuests();
        dto.ratePlan = reservation.getRatePlan().toString();
        dto.nights = reservation.getNights();
        dto.subtotal = reservation.getSubtotal();
        dto.discount = reservation.getDiscount();
        dto.total = reservation.getTotal();
        dto.status = reservation.getStatus().toString();
        dto.amountPaid = reservation.getAmountPaid();
        dto.refund = reservation.getRefund();
        return dto;
    }
}
