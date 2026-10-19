package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CnpjDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public Document generate(Reservation reservation, Room room, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        CnpjIdentification identification = (CnpjIdentification) reservation.getIdentification();
        BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        text.append("Empresa: ").append(identification.getCompanyName())
            .append(" (CNPJ ").append(identification.getCnpj()).append(")\n");
        text.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        text.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        text.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT))
            .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total)).append("\n");
        text.append("ISS retido (5%): ").append(formatMoney(iss));

        return new Document(
            "Fatura de hospedagem",
            reservation.getId(),
            reservation.getGuestName(),
            null,
            identification.getCnpj(),
            identification.getCompanyName(),
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
