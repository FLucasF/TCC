package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DocumentResponse {
    private String title;
    private long reservationId;
    private String guestName;
    private String guestCpf;
    private String roomNumber;
    private RoomType roomType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOut;

    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;

    public DocumentResponse(long reservationId, String guestName, String guestCpf,
                           String roomNumber, RoomType roomType, LocalDate checkIn,
                           LocalDate checkOut, int nights, BigDecimal subtotal,
                           BigDecimal discount, BigDecimal total) {
        this.title = "Comprovante de reserva";
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

    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
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
