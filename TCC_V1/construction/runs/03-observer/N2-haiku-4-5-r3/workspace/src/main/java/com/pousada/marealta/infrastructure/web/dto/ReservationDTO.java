package com.pousada.marealta.infrastructure.web.dto;

import com.pousada.marealta.domain.reservation.Reservation;
import java.time.LocalDate;

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
    public double subtotal;
    public double discount;
    public double total;
    public String status;
    public double amountPaid;
    public Double refund;

    public static ReservationDTO fromDomain(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.id = reservation.id();
        dto.roomNumber = reservation.room().number();
        dto.guestName = reservation.guest().name();
        dto.guestEmail = reservation.guest().email();
        dto.guestCpf = reservation.guest().cpf();
        dto.checkIn = reservation.dates().checkIn();
        dto.checkOut = reservation.dates().checkOut();
        dto.guests = reservation.guestCount();
        dto.ratePlan = reservation.ratePlan().displayName();
        dto.nights = reservation.dates().nights();
        dto.subtotal = reservation.subtotal().doubleValue();
        dto.discount = reservation.discount().doubleValue();
        dto.total = reservation.total().doubleValue();
        dto.status = reservation.status().displayName();
        dto.amountPaid = reservation.amountPaid().doubleValue();
        dto.refund = reservation.refund() != null ? reservation.refund().doubleValue() : null;
        return dto;
    }
}
