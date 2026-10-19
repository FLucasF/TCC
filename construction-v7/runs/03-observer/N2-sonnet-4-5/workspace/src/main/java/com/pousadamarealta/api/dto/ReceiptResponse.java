package com.pousadamarealta.api.dto;

import com.pousadamarealta.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceiptResponse {
    private String title;
    private long reservationId;
    private String guestName;
    private String guestCpf;
    private String roomNumber;
    private String roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;

    public static ReceiptResponse from(Reservation reservation, String roomType) {
        ReceiptResponse response = new ReceiptResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();
        response.guestCpf = reservation.getGuestCpf();
        response.roomNumber = reservation.getRoomNumber();
        response.roomType = roomType;
        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        return response;
    }

    public String getTitle() {
        return title;
    }

    public long getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
