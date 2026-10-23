package br.com.pousada.maralta.guesttype;

import br.com.pousada.maralta.domain.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GuestType {
    String getGuestIdentificationLine();

    Document generateDocument(
        long reservationId,
        String guestName,
        String roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    );

    String getGuestCpf();
    String getCompanyCnpj();
    String getCompanyName();
    String getGuestPassport();
    String getGuestCountry();
}
