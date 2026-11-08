package com.pousada.marealta.infrastructure.notification;

import com.pousada.marealta.domain.notification.DomainEvent;
import com.pousada.marealta.domain.notification.NotificationSubscriber;
import com.pousada.marealta.domain.notification.PersistedNotification;
import com.pousada.marealta.domain.notification.ReservationCancelled;
import com.pousada.marealta.domain.notification.ReservationConfirmed;
import com.pousada.marealta.domain.reservation.Reservation;

import java.time.format.DateTimeFormatter;

public class GuestNotifier implements NotificationSubscriber {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String name() {
        return "GuestNotifier";
    }

    @Override
    public PersistedNotification handle(DomainEvent event) {
        if (event instanceof ReservationConfirmed) {
            return handleConfirmed((ReservationConfirmed) event);
        } else if (event instanceof ReservationCancelled) {
            return handleCancelled((ReservationCancelled) event);
        }
        return null;
    }

    private PersistedNotification handleConfirmed(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        String receipt = buildReceipt(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation, receipt);

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                reservation.guest().email(),
                subject,
                body
        );
    }

    private PersistedNotification handleCancelled(ReservationCancelled event) {
        // Guest gets notified about cancellation
        Reservation reservation = event.reservation();

        String subject = "Reserva cancelada - Pousada Maré Alta";
        String body = "Sua reserva #" + reservation.id() + " foi cancelada.";

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                reservation.guest().email(),
                subject,
                body
        );
    }

    private String buildConfirmationEmail(Reservation reservation, String receipt) {
        return String.format(
                "Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!",
                reservation.guest().name(),
                receipt
        );
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id()).append("\n");
        sb.append("Hóspede: ").append(reservation.guest().name()).append(" (CPF ").append(reservation.guest().cpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.room().number()).append(" (").append(reservation.room().type().displayName()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.dates().checkIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(reservation.dates().checkOut())).append(" | ");
        sb.append(formatNights(reservation.dates().nights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.subtotal())).append("\n");

        if (reservation.discount().doubleValue() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.total()));

        return sb.toString();
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(com.pousada.marealta.domain.shared.Money money) {
        return String.format("R$ %.2f", money.doubleValue());
    }
}
