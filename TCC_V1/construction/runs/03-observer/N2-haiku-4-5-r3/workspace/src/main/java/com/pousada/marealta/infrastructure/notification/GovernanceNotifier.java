package com.pousada.marealta.infrastructure.notification;

import com.pousada.marealta.domain.notification.DomainEvent;
import com.pousada.marealta.domain.notification.NotificationSubscriber;
import com.pousada.marealta.domain.notification.PersistedNotification;
import com.pousada.marealta.domain.notification.ReservationCancelled;
import com.pousada.marealta.domain.notification.ReservationConfirmed;
import com.pousada.marealta.domain.reservation.Reservation;

import java.time.format.DateTimeFormatter;

public class GovernanceNotifier implements NotificationSubscriber {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    @Override
    public String name() {
        return "GovernanceNotifier";
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

        String subject = String.format("Reserva confirmada - quarto %d", reservation.room().number());
        String body = String.format(
                "Entrada: %s | Saída: %s | %d hóspedes",
                formatDate(reservation.dates().checkIn()),
                formatDate(reservation.dates().checkOut()),
                reservation.guestCount()
        );

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                GOVERNANCE_EMAIL,
                subject,
                body
        );
    }

    private PersistedNotification handleCancelled(ReservationCancelled event) {
        Reservation reservation = event.reservation();

        String subject = String.format("Reserva cancelada - quarto %d", reservation.room().number());
        String body = String.format(
                "Datas livres: %s a %s",
                formatDate(reservation.dates().checkIn()),
                formatDate(reservation.dates().checkOut())
        );

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                GOVERNANCE_EMAIL,
                subject,
                body
        );
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
