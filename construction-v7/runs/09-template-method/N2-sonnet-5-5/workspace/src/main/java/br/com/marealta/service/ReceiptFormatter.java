package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.time.format.DateTimeFormatter;

public final class ReceiptFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptFormatter() {}

    public static String format(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id()).append('\n');
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(formatCpf(r.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(r.room().number()).append(" (").append(r.room().type()).append(")\n");
        sb.append("Entrada: ").append(r.checkInDate().format(DATE))
                .append(" | Saída: ").append(r.checkOutDate().format(DATE))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount())).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total()));
        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
