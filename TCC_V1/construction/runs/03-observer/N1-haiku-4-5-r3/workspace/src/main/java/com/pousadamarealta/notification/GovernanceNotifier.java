package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.model.Message;

public class GovernanceNotifier implements ReservationNotifier {
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public GovernanceNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) throws Exception {
        String subject = String.format("Reserva confirmada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
                "Entrada: %s\nSaída: %s\nHóspedes: %d",
                DateUtils.formatDate(reservation.getCheckIn()),
                DateUtils.formatDate(reservation.getCheckOut()),
                reservation.getGuests()
        );

        Message message = new Message(0, "EMAIL", GOVERNANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) throws Exception {
        if (reservation.getAmountPaid().signum() == 0) {
            return;
        }

        String subject = String.format("Reserva cancelada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
                "Entrada: %s\nSaída: %s",
                DateUtils.formatDate(reservation.getCheckIn()),
                DateUtils.formatDate(reservation.getCheckOut())
        );

        Message message = new Message(0, "EMAIL", GOVERNANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }
}
