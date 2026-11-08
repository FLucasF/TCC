package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class GuestNotificationListener implements EventListener<ReservationConfirmedEvent> {
    private final MessageService messageService;

    public GuestNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(r);

        messageService.sendEmail(r.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmail(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(r.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildReceipt(r));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private String buildReceipt(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.getId()).append("\n");
        sb.append("Hóspede: ").append(r.getGuestName())
          .append(" (CPF ").append(Formatter.formatCpf(r.getGuestCpf())).append(")\n");

        String roomType = getRoomTypeName(r.getRoomNumber());
        sb.append("Quarto: ").append(r.getRoomNumber())
          .append(" (").append(roomType).append(")\n");

        sb.append("Entrada: ").append(Formatter.formatDate(r.getCheckIn()))
          .append(" | Saída: ").append(Formatter.formatDate(r.getCheckOut()))
          .append(" | ").append(r.getNights()).append(r.getNights() == 1 ? " diária" : " diárias").append("\n");

        sb.append("Diárias: ").append(Formatter.formatMoney(r.getSubtotal())).append("\n");

        if (r.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(Formatter.formatMoney(r.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatMoney(r.getTotal()));

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
}
