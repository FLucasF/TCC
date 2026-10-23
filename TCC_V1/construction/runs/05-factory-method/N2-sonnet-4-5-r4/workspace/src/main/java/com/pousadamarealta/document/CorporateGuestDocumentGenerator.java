package com.pousadamarealta.document;

import com.pousadamarealta.domain.GuestIdentification;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class CorporateGuestDocumentGenerator extends DocumentGenerator {
    private static final Locale LOCALE = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(LOCALE);
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    protected String formatTitle(long reservationId) {
        return "Fatura de hospedagem nº " + reservationId;
    }

    @Override
    protected String formatGuestIdentification(Reservation reservation) {
        GuestIdentification.CorporateGuest corporate =
            (GuestIdentification.CorporateGuest) reservation.guestIdentification();
        return "Empresa: " + corporate.companyName() + " (CNPJ " + corporate.cnpj() + ")\n" +
               "Hóspede: " + reservation.guestName();
    }

    @Override
    protected String formatRoom(int roomNumber, RoomType roomType) {
        return "Quarto: " + roomNumber + " (" + roomType + ")";
    }

    @Override
    protected String formatPeriod(LocalDate checkIn, LocalDate checkOut, int nights) {
        String nightsText = nights == 1 ? "1 diária" : nights + " diárias";
        return "Entrada: " + checkIn.format(DATE_FORMATTER) +
               " | Saída: " + checkOut.format(DATE_FORMATTER) +
               " | " + nightsText;
    }

    @Override
    protected String formatSubtotal(BigDecimal subtotal) {
        return "Diárias: " + CURRENCY_FORMATTER.format(subtotal);
    }

    @Override
    protected String formatDiscount(BigDecimal discount) {
        return "Desconto: " + CURRENCY_FORMATTER.format(discount);
    }

    @Override
    protected String formatTotal(BigDecimal total) {
        return "Total: " + CURRENCY_FORMATTER.format(total);
    }

    @Override
    protected String formatAdditionalFields(Reservation reservation) {
        BigDecimal iss = reservation.total().multiply(ISS_RATE)
                                          .setScale(2, RoundingMode.HALF_EVEN);
        return "ISS retido (5%): " + CURRENCY_FORMATTER.format(iss);
    }
}
