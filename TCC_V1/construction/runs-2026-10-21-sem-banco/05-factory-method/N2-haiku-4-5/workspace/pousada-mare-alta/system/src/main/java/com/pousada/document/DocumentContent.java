package com.pousada.document;

public record DocumentContent(
        String title,
        int reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        String roomType,
        String checkIn,
        String checkOut,
        int nights,
        double subtotal,
        double discount,
        double total,
        String text) {
}
