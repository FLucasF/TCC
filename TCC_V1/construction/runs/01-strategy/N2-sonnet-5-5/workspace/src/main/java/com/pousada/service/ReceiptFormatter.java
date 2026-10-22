package com.pousada.service;

import com.pousada.domain.Document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReceiptFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptFormatter() {
    }

    public static String format(Document d) {
        List<String> lines = new ArrayList<>();
        lines.add("Comprovante de reserva nº " + d.reservationId());
        lines.add("Hóspede: " + d.guestName() + " (CPF " + formatCpf(d.guestCpf()) + ")");
        lines.add("Quarto: " + d.roomNumber() + " (" + d.roomType() + ")");
        lines.add("Entrada: " + date(d.checkIn()) + " | Saída: " + date(d.checkOut()) + " | "
                + d.nights() + (d.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + money(d.subtotal()));
        if (d.discount().signum() > 0) {
            lines.add("Desconto: " + money(d.discount()));
        }
        lines.add("Total: " + money(d.total()));
        return String.join("\n", lines);
    }

    public static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    private static String date(LocalDate date) {
        return DATE.format(date);
    }

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
