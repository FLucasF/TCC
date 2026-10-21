package com.pousada.document;

import com.pousada.domain.CnpjIdentification;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class FaturaGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public String generate(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");

        CnpjIdentification cnpj = (CnpjIdentification) reservation.getIdentification();
        sb.append("Empresa: ").append(cnpj.companyName())
          .append(" (CNPJ ").append(formatCnpj(cnpj.cnpj())).append(")\n");

        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");

        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(reservation.getRoomType()).append(")\n");

        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT))
          .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal())).append("\n");

        BigDecimal iss = reservation.getTotal().multiply(ISS_RATE)
            .setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(formatMoney(iss));

        return sb.toString();
    }

    private String formatCnpj(String cnpj) {
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                   cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
        }
        return cnpj;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(java.util.Locale.of("pt", "BR"), "R$ %,.2f", amount);
    }
}
