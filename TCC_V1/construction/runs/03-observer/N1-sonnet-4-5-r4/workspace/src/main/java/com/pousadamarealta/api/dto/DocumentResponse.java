package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class DocumentResponse {
    @JsonProperty("title")
    private String title;

    @JsonProperty("reservationId")
    private Long reservationId;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("guestCpf")
    private String guestCpf;

    @JsonProperty("roomNumber")
    private String roomNumber;

    @JsonProperty("roomType")
    private String roomType;

    @JsonProperty("checkIn")
    private String checkIn;

    @JsonProperty("checkOut")
    private String checkOut;

    @JsonProperty("nights")
    private int nights;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("discount")
    private BigDecimal discount;

    @JsonProperty("total")
    private BigDecimal total;

    public DocumentResponse(Long reservationId, String guestName, String guestCpf,
                          String roomNumber, String roomType, String checkIn, String checkOut,
                          int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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

    public Long getReservationId() {
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

    public String getCheckIn() {
        return checkIn;
    }

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
