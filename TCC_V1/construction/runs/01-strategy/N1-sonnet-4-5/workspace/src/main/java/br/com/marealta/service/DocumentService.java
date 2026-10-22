package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(reservation.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getDateRange().checkIn().format(DATE_FORMATTER))
          .append(" | Saída: ").append(reservation.getDateRange().checkOut().format(DATE_FORMATTER))
          .append(" | ").append(formatNights(reservation.getDateRange().nights())).append("\n");

        Money subtotal = reservation.calculateSubtotal();
        Money discount = reservation.calculateDiscount();
        Money total = reservation.calculateTotal();

        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.isPositive()) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(total));

        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String formatMoney(Money money) {
        return String.format("R$ %,.2f", money.amount()).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
