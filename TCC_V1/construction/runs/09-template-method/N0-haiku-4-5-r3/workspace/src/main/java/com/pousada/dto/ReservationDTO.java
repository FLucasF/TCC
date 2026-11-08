package com.pousada.dto;

import com.pousada.models.Reservation;
import java.math.BigDecimal;
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
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String status;
    public BigDecimal amountPaid;
    public BigDecimal refund;

    public ReservationDTO(Reservation reservation) {
        this.id = reservation.id;
        this.roomNumber = reservation.roomNumber;
        this.guestName = reservation.guestName;
        this.guestEmail = reservation.guestEmail;
        this.guestCpf = reservation.guestCpf;
        this.checkIn = reservation.checkIn;
        this.checkOut = reservation.checkOut;
        this.guests = reservation.guests;
        this.ratePlan = reservation.ratePlan;
        this.nights = reservation.nights;
        this.subtotal = reservation.subtotal;
        this.discount = reservation.discount;
        this.total = reservation.total;
        this.status = reservation.status;
        this.amountPaid = reservation.amountPaid;
        this.refund = reservation.refund;
    }
}
