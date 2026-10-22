package com.pousadamarealta.util;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatReceipt(Reservation reservation, Room room) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(room.getType().name()).append(")\n");
        receipt.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
               .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
               .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.getTotal()));

        return receipt.toString();
    }

    public static String formatConfirmationEmail(String guestName, String receipt) {
        StringBuilder email = new StringBuilder();

        email.append("Olá, ").append(guestName).append("!\n\n");
        email.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        email.append(receipt).append("\n\n");
        email.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        email.append("Até breve!");

        return email.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", amount);
    }
}
