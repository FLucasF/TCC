package com.pousadamarealta.document;

import com.pousadamarealta.domain.GuestIdentification;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class IndividualGuestDocumentGenerator extends DocumentGenerator {
    private static final Locale LOCALE = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(LOCALE);

    @Override
    protected String formatTitle(long reservationId) {
        return "Comprovante de reserva nº " + reservationId;
    }

    @Override
    protected String formatGuestIdentification(Reservation reservation) {
        GuestIdentification.IndividualGuest guest =
            (GuestIdentification.IndividualGuest) reservation.guestIdentification();
        return "Hóspede: " + reservation.guestName() + " (CPF " + guest.cpf() + ")";
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
        return "";
    }
}
