package com.pousadamareal.reservas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    public int roomNumber;

    @JsonProperty("roomType")
    public String roomType;

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
}
