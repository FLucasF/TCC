package com.pousadamarealta.event.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationListener implements EventListener<ReservationConfirmedEvent> {
    private final NotificationService notificationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DecimalFormat moneyFormatter;

    public GuestNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        this.moneyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String receipt = buildReceipt(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", reservation.getGuestName(), receipt);

        notificationService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(reservation.getRoomNumber().startsWith("1") ? "STANDARD" :
                              reservation.getRoomNumber().startsWith("2") ? "DELUXE" : "SUITE").append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(dateFormatter))
          .append(" | Saída: ").append(reservation.getCheckOut().format(dateFormatter))
          .append(" | ").append(reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormatter.format(amount);
    }
}
