package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPaid() {
        return status.equals("CONFIRMED") || status.equals("CHECKED_IN") || status.equals("CHECKED_OUT");
    }
}
