package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.model.Reservation;
import com.pousada.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DocumentResponse {
    @JsonProperty("title")
    public String title;

    @JsonProperty("reservationId")
    public long reservationId;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("roomNumber")
    public String roomNumber;

    @JsonProperty("roomType")
    public RoomType roomType;

    @JsonProperty("checkIn")
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("nights")
    public long nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    public DocumentResponse(Reservation reservation, RoomType roomType) {
        this.title = "Comprovante de reserva";
        this.reservationId = reservation.getId();
        this.guestName = reservation.getGuestName();
        this.guestCpf = reservation.getGuestCpf();
        this.roomNumber = reservation.getRoomNumber();
        this.roomType = roomType;
        this.checkIn = reservation.getCheckIn();
        this.checkOut = reservation.getCheckOut();
        this.nights = reservation.getNights();
        this.subtotal = reservation.getSubtotal();
        this.discount = reservation.getDiscount();
        this.total = reservation.getTotal();
    }
}
