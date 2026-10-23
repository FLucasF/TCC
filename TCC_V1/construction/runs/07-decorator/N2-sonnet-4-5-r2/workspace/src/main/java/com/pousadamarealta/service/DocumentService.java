package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public String generateReceipt(Reservation reservation, String roomType) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(roomType).append(")\n");
        receipt.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
               .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return receipt.toString();
    }

    public String generateConfirmationEmail(Reservation reservation, String roomType) {
        String receipt = generateReceipt(reservation, roomType);

        StringBuilder email = new StringBuilder();
        email.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        email.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        email.append(receipt).append("\n\n");
        email.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        email.append("Até breve!");

        return email.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
