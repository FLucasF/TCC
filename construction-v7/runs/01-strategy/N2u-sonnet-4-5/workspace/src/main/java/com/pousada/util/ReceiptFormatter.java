package com.pousada.util;

import com.pousada.domain.Reservation;
import com.pousada.domain.RoomType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public static String formatReceipt(Reservation reservation, RoomType roomType) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(roomType).append(")\n");
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

    public static String formatConfirmationEmail(Reservation reservation, RoomType roomType) {
        String receipt = formatReceipt(reservation, roomType);

        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private static String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }

    private static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
