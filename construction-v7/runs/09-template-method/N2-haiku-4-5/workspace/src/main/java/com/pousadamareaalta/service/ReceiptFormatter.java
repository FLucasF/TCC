package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Receipt;
import java.time.format.DateTimeFormatter;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MONEY_FORMAT = "R$ %.2f";

    public String format(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", receipt.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", receipt.getGuestName(), formatCpf(receipt.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", receipt.getRoomNumber(), receipt.getRoomType()));

        String nightsLabel = receipt.getNights() == 1 ? "1 diária" : receipt.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            receipt.getCheckIn().format(DATE_FORMATTER),
            receipt.getCheckOut().format(DATE_FORMATTER),
            nightsLabel));

        sb.append(String.format("Diárias: " + MONEY_FORMAT + "\n", receipt.getSubtotal()));

        if (receipt.getDiscount().signum() > 0) {
            sb.append(String.format("Desconto: " + MONEY_FORMAT + "\n", receipt.getDiscount()));
        }

        sb.append(String.format("Total: " + MONEY_FORMAT, receipt.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9));
    }
}
