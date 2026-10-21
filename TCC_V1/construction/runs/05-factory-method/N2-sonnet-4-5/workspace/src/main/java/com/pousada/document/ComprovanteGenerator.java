package com.pousada.document;

import com.pousada.domain.CpfIdentification;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ComprovanteGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String generate(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");

        CpfIdentification cpf = (CpfIdentification) reservation.getIdentification();
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(cpf.cpf())).append(")\n");

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

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(java.util.Locale.of("pt", "BR"), "R$ %,.2f", amount);
    }
}
