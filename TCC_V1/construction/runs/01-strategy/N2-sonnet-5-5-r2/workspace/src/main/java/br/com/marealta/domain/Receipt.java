package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, RoomType roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append(TITLE).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(formattedCpf()).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(DATE.format(checkIn)).append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        text.append("Total: ").append(Money.brl(total));
        return text.toString();
    }

    private String formattedCpf() {
        if (guestCpf.matches("\\d{11}")) {
            return guestCpf.substring(0, 3) + "." + guestCpf.substring(3, 6) + "."
                    + guestCpf.substring(6, 9) + "-" + guestCpf.substring(9);
        }
        return guestCpf;
    }
}
