package com.pousadamarealta.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.time.format.DateTimeFormatter;

public class ReceiptResponse {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String title;
    private Long reservationId;
    private String guestName;
    private String guestCpf;
    private String roomNumber;
    private RoomType roomType;
    private String checkIn;
    private String checkOut;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;

    public static ReceiptResponse from(Reservation reservation, Room room) {
        ReceiptResponse response = new ReceiptResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();
        response.guestCpf = reservation.getGuestCpf();
        response.roomNumber = reservation.getRoomNumber();
        response.roomType = room.getType();
        response.checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
        response.checkOut = reservation.getCheckOut().format(DATE_FORMATTER);
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal().doubleValue();
        response.discount = reservation.getDiscount().doubleValue();
        response.total = reservation.getTotal().doubleValue();
        return response;
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("reservationId")
    public Long getReservationId() {
        return reservationId;
    }

    @JsonProperty("guestName")
    public String getGuestName() {
        return guestName;
    }

    @JsonProperty("guestCpf")
    public String getGuestCpf() {
        return guestCpf;
    }

    @JsonProperty("roomNumber")
    public String getRoomNumber() {
        return roomNumber;
    }

    @JsonProperty("roomType")
    public RoomType getRoomType() {
        return roomType;
    }

    @JsonProperty("checkIn")
    public String getCheckIn() {
        return checkIn;
    }

    @JsonProperty("checkOut")
    public String getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }
}
