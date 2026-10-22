package com.pousada.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentFormatter {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatCurrency(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }

    public static String formatCurrencyBRL(BigDecimal value) {
        return String.format("BRL %,.2f", value).replace(",", ".");
    }

    public static String formatBrDate(LocalDate date) {
        return date.format(BR_DATE);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String comprovante(long reservationId, String guestName, String guestCpf,
                                     int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                                     int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatBrDate(checkIn)).append(" | ");
        sb.append("Saída: ").append(formatBrDate(checkOut)).append(" | ");
        sb.append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(subtotal)).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(discount)).append("\n");
        }
        sb.append("Total: ").append(formatCurrency(total));
        return sb.toString();
    }

    public static String fatura(long reservationId, String guestName, String companyCnpj, String companyName,
                               int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                               int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        BigDecimal iss = Pricing.calculateIss(total);
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservationId).append("\n");
        sb.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        sb.append("Hóspede: ").append(guestName).append("\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatBrDate(checkIn)).append(" | ");
        sb.append("Saída: ").append(formatBrDate(checkOut)).append(" | ");
        sb.append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(subtotal)).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(discount)).append("\n");
        }
        sb.append("Total: ").append(formatCurrency(total)).append("\n");
        sb.append("ISS retido (5%): ").append(formatCurrency(iss));
        return sb.toString();
    }

    public static String bookingReceipt(long reservationId, String guestName, String guestPassport, String guestCountry,
                                       int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservationId).append("\n");
        sb.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
          .append(", ").append(guestCountry).append(")\n");
        sb.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Check-in: ").append(formatIsoDate(checkIn)).append(" | ");
        sb.append("Check-out: ").append(formatIsoDate(checkOut)).append(" | ");
        sb.append(nights).append(nights == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatCurrencyBRL(subtotal)).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatCurrencyBRL(discount)).append("\n");
        }
        sb.append("Total: ").append(formatCurrencyBRL(total));
        return sb.toString();
    }

    public static String emailComprovante(String guestName, String text) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               text + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }
}
