package com.pousada.marealta.infrastructure.notification;

import com.pousada.marealta.domain.notification.DomainEvent;
import com.pousada.marealta.domain.notification.NotificationSubscriber;
import com.pousada.marealta.domain.notification.PersistedNotification;
import com.pousada.marealta.domain.notification.ReservationCancelled;
import com.pousada.marealta.domain.notification.ReservationConfirmed;
import com.pousada.marealta.domain.reservation.Reservation;

public class FinanceNotifier implements NotificationSubscriber {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    @Override
    public String name() {
        return "FinanceNotifier";
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

        String subject = String.format("Pagamento recebido - reserva %d", reservation.id());
        String body = String.format("Valor: %s", formatMoney(reservation.total()));

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                FINANCE_EMAIL,
                subject,
                body
        );
    }

    private PersistedNotification handleCancelled(ReservationCancelled event) {
        if (event.refundAmount().doubleValue() <= 0) {
            return null;
        }

        Reservation reservation = event.reservation();

        String subject = String.format("Reembolso - reserva %d", reservation.id());
        String body = String.format("Valor: %s", formatMoney(event.refundAmount()));

        return PersistedNotification.success(
                reservation.id(),
                "EMAIL",
                FINANCE_EMAIL,
                subject,
                body
        );
    }

    private String formatMoney(com.pousada.marealta.domain.shared.Money money) {
        return String.format("R$ %.2f", money.doubleValue());
    }
}
