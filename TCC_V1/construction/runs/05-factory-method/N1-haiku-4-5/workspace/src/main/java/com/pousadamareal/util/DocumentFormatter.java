package com.pousadamareal.util;

import com.pousadamareal.model.Reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentFormatter {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatPTDate(LocalDate date) {
        return date.format(PT_DATE_FORMAT);
    }

    public static String formatENDate(LocalDate date) {
        return date.format(EN_DATE_FORMAT);
    }

    public static String formatPTCurrency(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }

    public static String formatENBRLCurrency(double value) {
        return String.format("BRL %.2f", value).replace(",", ".");
    }

    public static String generateComprovante(int reservationId, Reservation res, String roomType) {
        LocalDate checkIn = LocalDate.parse(res.checkIn);
        LocalDate checkOut = LocalDate.parse(res.checkOut);

        String nightsLabel = res.nights == 1 ? "1 diária" : res.nights + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(res.guestName).append(" (CPF ").append(res.guestCpf).append(")\n");
        sb.append("Quarto: ").append(res.roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatPTDate(checkIn)).append(" | Saída: ").append(formatPTDate(checkOut)).append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(formatPTCurrency(res.subtotal)).append("\n");

        if (res.discount > 0) {
            sb.append("Desconto: ").append(formatPTCurrency(res.discount)).append("\n");
        }

        sb.append("Total: ").append(formatPTCurrency(res.total));

        return sb.toString();
    }

    public static String generateFatura(int reservationId, Reservation res, String roomType) {
        LocalDate checkIn = LocalDate.parse(res.checkIn);
        LocalDate checkOut = LocalDate.parse(res.checkOut);
        double iss = PriceCalculator.round(res.total * 0.05);

        String nightsLabel = res.nights == 1 ? "1 diária" : res.nights + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservationId).append("\n");
        sb.append("Empresa: ").append(res.companyName).append(" (CNPJ ").append(res.companyCnpj).append(")\n");
        sb.append("Hóspede: ").append(res.guestName).append("\n");
        sb.append("Quarto: ").append(res.roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatPTDate(checkIn)).append(" | Saída: ").append(formatPTDate(checkOut)).append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(formatPTCurrency(res.subtotal)).append("\n");

        if (res.discount > 0) {
            sb.append("Desconto: ").append(formatPTCurrency(res.discount)).append("\n");
        }

        sb.append("Total: ").append(formatPTCurrency(res.total)).append("\n");
        sb.append("ISS retido (5%): ").append(formatPTCurrency(iss));

        return sb.toString();
    }

    public static String generateBookingReceipt(int reservationId, Reservation res, String roomType) {
        LocalDate checkIn = LocalDate.parse(res.checkIn);
        LocalDate checkOut = LocalDate.parse(res.checkOut);

        String nightsLabel = res.nights == 1 ? "1 night" : res.nights + " nights";
        String passportInfo = String.format("passport %s, %s", res.guestPassport, res.guestCountry);

        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservationId).append("\n");
        sb.append("Guest: ").append(res.guestName).append(" (").append(passportInfo).append(")\n");
        sb.append("Room: ").append(res.roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Check-in: ").append(formatENDate(checkIn)).append(" | Check-out: ").append(formatENDate(checkOut)).append(" | ").append(nightsLabel).append("\n");
        sb.append("Nights: ").append(formatENBRLCurrency(res.subtotal)).append("\n");

        if (res.discount > 0) {
            sb.append("Discount: ").append(formatENBRLCurrency(res.discount)).append("\n");
        }

        sb.append("Total: ").append(formatENBRLCurrency(res.total));

        return sb.toString();
    }

    public static String generateConfirmationEmail(String guestName, String documentText) {
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", guestName, documentText);
    }
}
