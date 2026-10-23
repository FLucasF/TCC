package com.pousadamarealta.document;

import com.pousadamarealta.domain.GuestIdentification;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Document(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String guestPassport,
    String guestCountry,
    String companyCnpj,
    String companyName,
    int roomNumber,
    RoomType roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String text
) {
    public static Document from(long reservationId, String guestName, GuestIdentification guestId,
                                int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                                int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                                String text) {
        String cpf = null, passport = null, country = null, cnpj = null, companyName = null;

        if (guestId instanceof GuestIdentification.IndividualGuest individual) {
            cpf = individual.cpf();
        } else if (guestId instanceof GuestIdentification.ForeignGuest foreign) {
            passport = foreign.passport();
            country = foreign.country();
        } else if (guestId instanceof GuestIdentification.CorporateGuest corporate) {
            cnpj = corporate.cnpj();
            companyName = corporate.companyName();
        }

        return new Document(null, reservationId, guestName, cpf, passport, country, cnpj, companyName,
                          roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total, text);
    }
}
