package com.mareaalta.service;

import com.mareaalta.domain.Reservation;
import com.mareaalta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentGenerator {
    private static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String generateComprovante(Reservation res, Room room) {
        String checkInStr = res.getCheckIn().format(BR_DATE_FORMAT);
        String checkOutStr = res.getCheckOut().format(BR_DATE_FORMAT);
        String nightsLabel = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.getId()).append("\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append(" (CPF ").append(res.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(res.getRoomNumber()).append(" (").append(room.getType().name()).append(")\n");
        sb.append("Entrada: ").append(checkInStr).append(" | Saída: ").append(checkOutStr).append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(formatCurrency(res.getSubtotal())).append("\n");

        if (res.getDiscount() != null && res.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatCurrency(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(res.getTotal())).append("\n");

        return sb.toString();
    }

    public static String generateFatura(Reservation res, Room room) {
        String checkInStr = res.getCheckIn().format(BR_DATE_FORMAT);
        String checkOutStr = res.getCheckOut().format(BR_DATE_FORMAT);
        String nightsLabel = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";

        BigDecimal iss = res.getTotal().multiply(new BigDecimal("0.05"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(res.getId()).append("\n");
        sb.append("Empresa: ").append(res.getCompanyName()).append(" (CNPJ ").append(res.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append("\n");
        sb.append("Quarto: ").append(res.getRoomNumber()).append(" (").append(room.getType().name()).append(")\n");
        sb.append("Entrada: ").append(checkInStr).append(" | Saída: ").append(checkOutStr).append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(formatCurrency(res.getSubtotal())).append("\n");

        if (res.getDiscount() != null && res.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatCurrency(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(res.getTotal())).append("\n");
        sb.append("ISS retido (5%): ").append(formatCurrency(iss)).append("\n");

        return sb.toString();
    }

    public static String generateBookingReceipt(Reservation res, Room room) {
        String checkInStr = res.getCheckIn().format(ISO_DATE_FORMAT);
        String checkOutStr = res.getCheckOut().format(ISO_DATE_FORMAT);
        String nightsLabel = res.getNights() == 1 ? "1 night" : res.getNights() + " nights";

        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(res.getId()).append("\n");
        sb.append("Guest: ").append(res.getGuestName()).append(" (passport ").append(res.getGuestPassport())
            .append(", ").append(res.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(res.getRoomNumber()).append(" (").append(room.getType().name()).append(")\n");
        sb.append("Check-in: ").append(checkInStr).append(" | Check-out: ").append(checkOutStr).append(" | ").append(nightsLabel).append("\n");
        sb.append("Nights: ").append(formatCurrencyEnglish(res.getSubtotal())).append("\n");

        if (res.getDiscount() != null && res.getDiscount().signum() > 0) {
            sb.append("Discount: ").append(formatCurrencyEnglish(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrencyEnglish(res.getTotal())).append("\n");

        return sb.toString();
    }

    public static String generateConfirmationEmail(String guestName, String document) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               document +
               "\nA entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!\n";
    }

    private static String formatCurrency(BigDecimal value) {
        String formatted = String.format("%.2f", value);
        String[] parts = formatted.split("\\.");
        String intPart = parts[0];
        String decPart = parts[1];

        StringBuilder result = new StringBuilder();
        int length = intPart.length();
        for (int i = 0; i < length; i++) {
            if (i > 0 && (length - i) % 3 == 0) {
                result.append(".");
            }
            result.append(intPart.charAt(i));
        }

        return "R$ " + result.toString() + "," + decPart;
    }

    private static String formatCurrencyEnglish(BigDecimal value) {
        return "BRL " + String.format("%.2f", value);
    }
}
