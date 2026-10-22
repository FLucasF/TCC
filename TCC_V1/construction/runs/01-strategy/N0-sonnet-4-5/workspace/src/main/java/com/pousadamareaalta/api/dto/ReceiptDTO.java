package com.pousadamareaalta.api.dto;

import java.math.BigDecimal;

public record ReceiptDTO(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    int roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
}
