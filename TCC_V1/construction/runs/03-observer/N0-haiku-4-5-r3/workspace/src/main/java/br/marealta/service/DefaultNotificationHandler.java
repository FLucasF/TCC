package br.marealta.service;

import br.marealta.model.MessageChannel;
import br.marealta.model.Reservation;
import br.marealta.util.Formatters;

public class DefaultNotificationHandler implements NotificationHandler {
    private final NotificationService notificationService;

    public DefaultNotificationHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        notifyGovernance(reservation);
        notifyFinance(reservation);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        notifyGovernanceCancellation(reservation);
        if (reservation.getRefund() != null && reservation.getRefund().compareTo(java.math.BigDecimal.ZERO) > 0) {
            notifyFinanceCancellation(reservation);
        }
    }

    private void notifyGovernance(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %d", reservation.getRoomNumber());
        String body = String.format("Quarto %d\nEntrada: %s\nSaída: %s\nHóspedes: %d",
            reservation.getRoomNumber(),
            Formatters.formatDate(reservation.getCheckIn()),
            Formatters.formatDate(reservation.getCheckOut()),
            reservation.getGuests());

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        notificationService.send(notification);
    }

    private void notifyFinance(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format("Valor recebido: %s", Formatters.formatMoney(reservation.getAmountPaid()));

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            subject,
            body
        );
        notificationService.send(notification);
    }

    private void notifyGovernanceCancellation(Reservation reservation) {
        String subject = String.format("Reserva cancelada - quarto %d", reservation.getRoomNumber());
        String body = String.format("Quarto %d\nEntrada: %s\nSaída: %s",
            reservation.getRoomNumber(),
            Formatters.formatDate(reservation.getCheckIn()),
            Formatters.formatDate(reservation.getCheckOut()));

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        notificationService.send(notification);
    }

    private void notifyFinanceCancellation(Reservation reservation) {
        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format("Valor a devolver: %s", Formatters.formatMoney(reservation.getRefund()));

        Notification notification = new Notification(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            subject,
            body
        );
        notificationService.send(notification);
    }
}
