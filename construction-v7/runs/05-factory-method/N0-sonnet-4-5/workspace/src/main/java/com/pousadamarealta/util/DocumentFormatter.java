package com.pousadamarealta.util;

import com.pousadamarealta.model.GuestIdentification;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {

    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification id = reservation.getIdentification();

        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(), id.getGuestCpf()));
        sb.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), room.getType()));
        sb.append(formatCheckInOut(reservation.getCheckIn(), reservation.getCheckOut(),
            reservation.getNights(), "pt"));
        sb.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal(), "pt")));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount(), "pt")));
        }

        sb.append(String.format("Total: %s", formatMoney(reservation.getTotal(), "pt")));

        return sb.toString();
    }

    public static String formatInvoice(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification id = reservation.getIdentification();

        sb.append(String.format("Fatura de hospedagem nº %d%n", reservation.getId()));
        sb.append(String.format("Empresa: %s (CNPJ %s)%n",
            id.getCompanyName(), id.getCompanyCnpj()));
        sb.append(String.format("Hóspede: %s%n", reservation.getGuestName()));
        sb.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), room.getType()));
        sb.append(formatCheckInOut(reservation.getCheckIn(), reservation.getCheckOut(),
            reservation.getNights(), "pt"));
        sb.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal(), "pt")));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount(), "pt")));
        }

        sb.append(String.format("Total: %s%n", formatMoney(reservation.getTotal(), "pt")));

        BigDecimal iss = PriceCalculator.calculateIss(reservation.getTotal());
        sb.append(String.format("ISS retido (5%%): %s", formatMoney(iss, "pt")));

        return sb.toString();
    }

    public static String formatBookingReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification id = reservation.getIdentification();

        sb.append(String.format("Booking receipt no. %d%n", reservation.getId()));
        sb.append(String.format("Guest: %s (passport %s, %s)%n",
            reservation.getGuestName(), id.getGuestPassport(), id.getGuestCountry()));
        sb.append(String.format("Room: %s (%s)%n",
            reservation.getRoomNumber(), room.getType()));
        sb.append(formatCheckInOut(reservation.getCheckIn(), reservation.getCheckOut(),
            reservation.getNights(), "en"));
        sb.append(String.format("Nights: %s%n", formatMoney(reservation.getSubtotal(), "en")));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Discount: %s%n", formatMoney(reservation.getDiscount(), "en")));
        }

        sb.append(String.format("Total: %s", formatMoney(reservation.getTotal(), "en")));

        return sb.toString();
    }

    private static String formatCheckInOut(LocalDate checkIn, LocalDate checkOut, int nights, String lang) {
        String checkInStr, checkOutStr, nightsLabel;

        if ("en".equals(lang)) {
            checkInStr = checkIn.format(EN_DATE_FORMAT);
            checkOutStr = checkOut.format(EN_DATE_FORMAT);
            nightsLabel = nights == 1 ? "1 night" : nights + " nights";
        } else {
            checkInStr = checkIn.format(PT_DATE_FORMAT);
            checkOutStr = checkOut.format(PT_DATE_FORMAT);
            nightsLabel = nights == 1 ? "1 diária" : nights + " diárias";
        }

        if ("en".equals(lang)) {
            return String.format("Check-in: %s | Check-out: %s | %s%n",
                checkInStr, checkOutStr, nightsLabel);
        } else {
            return String.format("Entrada: %s | Saída: %s | %s%n",
                checkInStr, checkOutStr, nightsLabel);
        }
    }

    private static String formatMoney(BigDecimal amount, String lang) {
        if ("en".equals(lang)) {
            return String.format(Locale.US, "BRL %,.2f", amount);
        } else {
            String formatted = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", amount);
            formatted = formatted.replace('.', ',');
            return "R$ " + formatted;
        }
    }

    public static String formatConfirmationEmail(String guestName, String documentText) {
        return String.format(
            "Olá, %s!%n%n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:%n%n" +
            "%s%n%n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.%n" +
            "Até breve!",
            guestName, documentText
        );
    }
}
