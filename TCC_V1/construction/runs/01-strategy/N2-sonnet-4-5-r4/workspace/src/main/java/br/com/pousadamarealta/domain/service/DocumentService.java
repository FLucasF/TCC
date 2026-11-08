package br.com.pousadamarealta.domain.service;

import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public String generateReceipt(Reservation reservation, Room room) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(room.getType().name()).append(")\n");
        receipt.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
               .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(CURRENCY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(CURRENCY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(CURRENCY_FORMAT.format(reservation.getTotal()));

        return receipt.toString();
    }

    public String generateConfirmationEmail(Reservation reservation, Room room) {
        String receipt = generateReceipt(reservation, room);

        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
