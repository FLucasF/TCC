package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GuestNotificationHandler implements NotificationHandler {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private long messageIdCounter = 1;

    @Override
    public List<NotificationMessage> handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            return List.of(createConfirmationEmail(confirmed.reservation()));
        }
        return Collections.emptyList();
    }

    private NotificationMessage createConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String receipt = formatReceipt(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        return new NotificationMessage(
            messageIdCounter++,
            "EMAIL",
            reservation.getGuestEmail(),
            subject,
            body
        );
    }

    private String formatReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(reservation.getRoomType()).append(")\n");
        receipt.append("Entrada: ").append(DATE_FORMAT.format(reservation.getCheckIn()))
               .append(" | Saída: ").append(DATE_FORMAT.format(reservation.getCheckOut()))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.getTotal()));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return "R$ " + format.format(amount);
    }

    public void setMessageIdCounter(long counter) {
        this.messageIdCounter = counter;
    }
}
