package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Document;

import java.math.BigDecimal;

public class DocumentResponse {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final int roomNumber;
    private final String roomType;
    private final String checkIn;
    private final String checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public DocumentResponse(Document document) {
        this.title = document.getTitle();
        this.reservationId = document.getReservationId();
        this.guestName = document.getGuestName();
        this.guestCpf = document.getGuestCpf();
        this.roomNumber = document.getRoomNumber();
        this.roomType = document.getRoomType().name();
        this.checkIn = document.getCheckIn().toString();
        this.checkOut = document.getCheckOut().toString();
        this.nights = document.getNights();
        this.subtotal = document.getSubtotal();
        this.discount = document.getDiscount();
        this.total = document.getTotal();
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("reservationId")
    public long getReservationId() {
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
    public int getRoomNumber() {
        return roomNumber;
    }

    @JsonProperty("roomType")
    public String getRoomType() {
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
