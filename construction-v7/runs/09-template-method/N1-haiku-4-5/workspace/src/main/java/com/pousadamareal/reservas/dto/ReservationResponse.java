package com.pousadamareal.reservas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    @JsonProperty("id")
    public long id;

    @JsonProperty("roomNumber")
    public int roomNumber;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestEmail")
    public String guestEmail;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("checkIn")
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("guests")
    public int guests;

    @JsonProperty("ratePlan")
    public String ratePlan;

    @JsonProperty("nights")
    public long nights;

    @JsonProperty("subtotal")
    public BigDecimal subtotal;

    @JsonProperty("discount")
    public BigDecimal discount;

    @JsonProperty("total")
    public BigDecimal total;

    @JsonProperty("status")
    public String status;

    @JsonProperty("amountPaid")
    public BigDecimal amountPaid;

    @JsonProperty("refund")
    public BigDecimal refund;
}
