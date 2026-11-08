package com.pousadamareaalta.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Receipt(
    @JsonProperty("title")
    String title,
    @JsonProperty("reservationId")
    long reservationId,
    @JsonProperty("guestName")
    String guestName,
    @JsonProperty("guestCpf")
    String guestCpf,
    @JsonProperty("roomNumber")
    int roomNumber,
    @JsonProperty("roomType")
    String roomType,
    @JsonProperty("checkIn")
    LocalDate checkIn,
    @JsonProperty("checkOut")
    LocalDate checkOut,
    @JsonProperty("nights")
    int nights,
    @JsonProperty("subtotal")
    double subtotal,
    @JsonProperty("discount")
    double discount,
    @JsonProperty("total")
    double total
) {}
