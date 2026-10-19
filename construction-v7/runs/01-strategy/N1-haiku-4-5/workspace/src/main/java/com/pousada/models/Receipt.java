package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Receipt {
    private final String title = "Comprovante de reserva";
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final int roomNumber;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public Receipt(long reservationId, String guestName, String guestCpf, int roomNumber,
                   String roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
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

    public int getRoomNumber() {
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
