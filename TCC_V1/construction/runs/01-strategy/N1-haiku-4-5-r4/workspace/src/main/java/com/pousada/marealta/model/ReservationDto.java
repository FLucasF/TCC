package com.pousada.marealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDto {
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

    public ReservationDto(Reservation r) {
        this.id = r.getId();
        this.roomNumber = r.getRoomNumber();
        this.guestName = r.getGuestName();
        this.guestEmail = r.getGuestEmail();
        this.guestCpf = r.getGuestCpf();
        this.checkIn = r.getCheckIn();
        this.checkOut = r.getCheckOut();
        this.guests = r.getGuests();
        this.ratePlan = r.getRatePlan().name();
        this.nights = r.getNights();
        this.subtotal = r.getSubtotal();
        this.discount = r.getDiscount();
        this.total = r.getTotal();
        this.status = r.getStatus().name();
        this.amountPaid = r.getAmountPaid();
        this.refund = r.getRefund();
    }
}
