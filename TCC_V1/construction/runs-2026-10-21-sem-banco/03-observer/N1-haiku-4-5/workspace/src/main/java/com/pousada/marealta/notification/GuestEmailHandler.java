package com.pousada.marealta.notification;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.util.FormatHelper;

public class GuestEmailHandler implements NotificationHandler {
    private final MessageService messageService;

    public GuestEmailHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handleConfirmation(Reservation reservation) {
        String receipt = buildReceipt(reservation);
        String body = buildEmail(reservation, receipt);

        messageService.sendMessage(
            "EMAIL",
            reservation.guestEmail,
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    @Override
    public void handleCancellation(Reservation reservation) {
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName)
            .append(" (CPF ").append(reservation.guestCpf).append(")\n");
        receipt.append("Quarto: ").append(reservation.roomNumber)
            .append(" (").append(getRoomType(reservation.roomNumber)).append(")\n");
        receipt.append("Entrada: ").append(FormatHelper.formatBrDate(reservation.checkIn))
            .append(" | Saída: ").append(FormatHelper.formatBrDate(reservation.checkOut))
            .append(" | ").append(reservation.nights).append(reservation.nights == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(FormatHelper.formatCurrency(reservation.subtotal)).append("\n");

        if (reservation.discount.signum() > 0) {
            receipt.append("Desconto: ").append(FormatHelper.formatCurrency(reservation.discount)).append("\n");
        }

        receipt.append("Total: ").append(FormatHelper.formatCurrency(reservation.total)).append("\n");

        return receipt.toString();
    }

    private String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private String buildEmail(Reservation reservation, String receipt) {
        return "Olá, " + reservation.guestName + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            receipt + "\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";
    }
}
