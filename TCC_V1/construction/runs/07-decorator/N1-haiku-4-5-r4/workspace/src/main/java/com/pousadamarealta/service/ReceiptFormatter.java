package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.pousadamarealta.models.Receipt;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.RoomType;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(PT_BR);

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }

    public static String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.reservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName()).append(" (CPF ").append(formatCpf(receipt.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber()).append(" (").append(receipt.roomType()).append(")\n");
        sb.append("Entrada: ").append(DATE_FORMATTER.format(receipt.checkIn())).append(" | ");
        sb.append("Saída: ").append(DATE_FORMATTER.format(receipt.checkOut())).append(" | ");
        sb.append(receipt.nights()).append(receipt.nights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(receipt.subtotal())).append("\n");

        if (receipt.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(receipt.discount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(receipt.total()));
        return sb.toString();
    }

    public static String formatConfirmationEmail(String guestName, Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(formatReceipt(receipt)).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public static Receipt toReceipt(int reservationId, String guestName, String guestCpf, int roomNumber,
                                    RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                                    BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        return new Receipt("Comprovante de reserva", reservationId, guestName, guestCpf, roomNumber,
                          roomType, checkIn, checkOut, nights, subtotal, discount, total);
    }

    private static String formatCurrency(BigDecimal value) {
        String formatted = CURRENCY_FORMAT.format(value);
        return "R$ " + formatted;
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
