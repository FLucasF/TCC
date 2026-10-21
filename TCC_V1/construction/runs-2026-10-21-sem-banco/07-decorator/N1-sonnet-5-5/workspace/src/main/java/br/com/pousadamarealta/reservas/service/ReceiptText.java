package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.Money;
import java.time.format.DateTimeFormatter;

/** O comprovante como texto, para o corpo do e-mail. */
public final class ReceiptText {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptText() {
    }

    public static String of(Receipt receipt) {
        StringBuilder text = new StringBuilder();
        text.append(Receipt.TITLE).append(" nº ").append(receipt.reservationId()).append('\n');
        text.append("Hóspede: ").append(receipt.guestName())
                .append(" (CPF ").append(receipt.guestCpf()).append(")\n");
        text.append("Quarto: ").append(receipt.roomNumber()).append(" (").append(receipt.roomType()).append(")\n");
        text.append("Entrada: ").append(DATE.format(receipt.checkIn()))
                .append(" | Saída: ").append(DATE.format(receipt.checkOut()))
                .append(" | ").append(receipt.nights()).append(receipt.nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(Money.format(receipt.subtotal())).append('\n');
        if (receipt.discount().signum() > 0) {
            text.append("Desconto: ").append(Money.format(receipt.discount())).append('\n');
        }
        text.append("Total: ").append(Money.format(receipt.total()));
        return text.toString();
    }
}
