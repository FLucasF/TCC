package com.pousada.marealta.notification.notifiers;

import com.pousada.marealta.notification.ReservationEvent;
import com.pousada.marealta.notification.ReservationNotifier;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.enums.MessageChannel;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.util.Formatter;
import java.math.BigDecimal;

public class FinanceNotifier implements ReservationNotifier {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    @Override
    public void handleEvent(ReservationEvent event) throws Exception {
        Reservation reservation = event.getReservation();

        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            String subject = "Pagamento recebido - reserva " + reservation.getId();
            String body = String.format(
                "Reserva: %d\nValor: %s",
                reservation.getId(),
                Formatter.formatMoney(reservation.getTotal())
            );

            MessageRepository.create(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
        } else if (event.getType() == ReservationEvent.Type.CANCELLED) {
            if (reservation.getRefund() != null && reservation.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                String subject = "Reembolso - reserva " + reservation.getId();
                String body = String.format(
                    "Reserva: %d\nValor a devolver: %s",
                    reservation.getId(),
                    Formatter.formatMoney(reservation.getRefund())
                );

                MessageRepository.create(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
            }
        }
    }
}
