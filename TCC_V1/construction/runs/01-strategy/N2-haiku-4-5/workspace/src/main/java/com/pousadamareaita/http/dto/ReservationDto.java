package com.pousadamareaita.http.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationDto {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public int id;
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

    public ReservationDto(com.pousadamareaita.model.Reservation res) {
        this.id = res.getId();
        this.roomNumber = res.getRoomNumber();
        this.guestName = res.getGuestName();
        this.guestEmail = res.getGuestEmail();
        this.guestCpf = res.getGuestCpf();
        this.checkIn = res.getCheckIn().format(DATE_FORMATTER);
        this.checkOut = res.getCheckOut().format(DATE_FORMATTER);
        this.guests = res.getGuests();
        this.ratePlan = res.getRatePlan().toString();
        this.nights = res.getNights();
        this.subtotal = res.getSubtotal().doubleValue();
        this.discount = res.getDiscount().doubleValue();
        this.total = res.getTotal().doubleValue();
        this.status = res.getStatus().toString();
        this.amountPaid = res.getAmountPaid().doubleValue();
        this.refund = res.getRefund() != null ? res.getRefund().doubleValue() : null;
    }
}
