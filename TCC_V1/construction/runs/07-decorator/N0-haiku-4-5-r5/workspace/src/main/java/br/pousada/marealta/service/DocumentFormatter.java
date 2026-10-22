package br.pousada.marealta.service;

import br.pousada.marealta.model.Document;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String format(Document doc) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Comprovante de reserva nº %d\n", doc.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", doc.getGuestName(), formatCpf(doc.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", doc.getRoomNumber(), doc.getRoomType()));

        String nights = doc.getNights() == 1 ? "1 diária" : doc.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            formatDate(doc.getCheckIn()),
            formatDate(doc.getCheckOut()),
            nights));

        sb.append(String.format("Diárias: %s\n", formatMoney(doc.getSubtotal())));

        if (doc.getDiscount() != null && doc.getDiscount() > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(doc.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(doc.getTotal())));

        return sb.toString();
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private static String formatMoney(double value) {
        String formatted = String.format("%.2f", value);
        String[] parts = formatted.split("\\.");
        String intPart = parts[0];
        String decPart = parts[1];

        StringBuilder result = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                result.append(".");
            }
            result.append(intPart.charAt(i));
        }
        return "R$ " + result + "," + decPart;
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9));
        }
        return cpf;
    }
}
