package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Document(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    RoomType roomType,
    LocalDate checkIn,
    LocalDate checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static Document fromReservation(Reservation reservation) {
        return new Document(
            "Comprovante de reserva",
            reservation.id(),
            reservation.guestName(),
            reservation.guestCpf(),
            reservation.roomNumber(),
            reservation.roomType(),
            reservation.checkIn(),
            reservation.checkOut(),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total()
        );
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(checkIn))
          .append(" | Saída: ").append(formatDate(checkOut))
          .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }
        sb.append("Total: ").append(formatMoney(total));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.');
    }
}
