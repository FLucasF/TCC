package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.00", SYMBOLS);

    public static String generateReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        receipt.append("Quarto: ").append(reservation.roomNumber).append(" (").append(reservation.roomType).append(")\n");
        receipt.append("Entrada: ").append(reservation.checkIn.format(DATE_FORMATTER)).append(" | ");
        receipt.append("Saída: ").append(reservation.checkOut.format(DATE_FORMATTER)).append(" | ");
        receipt.append(reservation.nights).append(" diária").append(reservation.nights > 1 ? "s" : "").append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.discount)).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.total));

        return receipt.toString();
    }

    public static class ReceiptResponse {
        public String title = "Comprovante de reserva";
        public int reservationId;
        public String guestName;
        public String guestCpf;
        public int roomNumber;
        public String roomType;
        public String checkIn;
        public String checkOut;
        public int nights;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public ReceiptResponse(Reservation reservation) {
            this.reservationId = reservation.id;
            this.guestName = reservation.guestName;
            this.guestCpf = formatCpf(reservation.guestCpf);
            this.roomNumber = reservation.roomNumber;
            this.roomType = reservation.roomType;
            this.checkIn = reservation.checkIn.format(DATE_FORMATTER);
            this.checkOut = reservation.checkOut.format(DATE_FORMATTER);
            this.nights = reservation.nights;
            this.subtotal = reservation.subtotal;
            this.discount = reservation.discount;
            this.total = reservation.total;
        }
    }

    private static String formatMoney(BigDecimal value) {
        return "R$ " + MONEY_FORMAT.format(value).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
