package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDTO(
    long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int guests,
    @JsonProperty("ratePlan") String ratePlan,
    long nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    BigDecimal refund
) {}
