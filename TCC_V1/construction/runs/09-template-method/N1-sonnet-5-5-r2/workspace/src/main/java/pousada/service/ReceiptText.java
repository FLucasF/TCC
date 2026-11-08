package pousada.service;

import pousada.domain.Receipt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReceiptText {
    private ReceiptText() {
    }

    public static String format(Receipt receipt) {
        List<String> lines = new ArrayList<>();
        lines.add(receipt.title() + " nº " + receipt.reservationId());
        lines.add("Hóspede: " + receipt.guestName() + " (CPF " + receipt.guestCpf() + ")");
        lines.add("Quarto: " + receipt.roomNumber() + " (" + receipt.roomType() + ")");
        lines.add("Entrada: " + BrazilianFormat.date(receipt.checkIn())
                + " | Saída: " + BrazilianFormat.date(receipt.checkOut())
                + " | " + receipt.nights() + (receipt.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + BrazilianFormat.money(receipt.subtotal()));
        if (receipt.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add("Desconto: " + BrazilianFormat.money(receipt.discount()));
        }
        lines.add("Total: " + BrazilianFormat.money(receipt.total()));
        return String.join("\n", lines);
    }
}
