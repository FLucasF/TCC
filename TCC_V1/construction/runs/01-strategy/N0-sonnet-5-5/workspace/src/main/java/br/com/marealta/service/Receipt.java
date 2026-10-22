package br.com.marealta.service;

import br.com.marealta.plan.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva (também usado como segunda via). */
public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(BR_DATE.format(checkIn)).append(" | Saída: ").append(BR_DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(total));
        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
