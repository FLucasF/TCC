package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GuestIdentification {
    String getGuestCpf();
    String getCompanyCnpj();
    String getCompanyName();
    String getGuestPassport();
    String getGuestCountry();

    Document generateDocument(
        long reservationId,
        String guestName,
        String roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    );
}
