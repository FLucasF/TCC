package com.marealta.application.handler;

import com.marealta.domain.event.ReservationConfirmedEvent;
import com.marealta.domain.model.Reservation;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.notification.EventHandler;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationHandler implements EventHandler<ReservationConfirmedEvent> {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);

    private final EmailService emailService;

    public GuestNotificationHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String receipt = buildReceipt(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        emailService.send(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")").append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (")
          .append(getRoomTypeName(reservation.getRoomNumber())).append(")").append("\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));
        return sb.toString();
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
