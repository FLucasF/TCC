package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CpfDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public Document generate(Reservation reservation, Room room, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        CpfIdentification identification = (CpfIdentification) reservation.getIdentification();

        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        text.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(identification.getCpf()).append(")\n");
        text.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        text.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT))
            .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total));

        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            identification.getCpf(),
            null,
            null,
            null,
            null,
            room.getNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            subtotal,
            discount,
            total,
            text.toString()
        );
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }
}
