package br.com.pousadamarealta.listeners;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.domain.events.ReservationCancelled;
import br.com.pousadamarealta.domain.events.ReservationConfirmed;
import br.com.pousadamarealta.domain.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;

public final class HousekeepingNotifier implements ReservationEventListener {
    private final MessageRepository messageRepository;

    public HousekeepingNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        String body = String.format("""
            Reserva confirmada para o quarto %s.

            Entrada: %s
            Saída: %s
            Hóspedes: %d""",
            reservation.roomNumber(),
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()),
            reservation.guests());

        Message message = new Message(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            String.format("Reserva confirmada - quarto %s", reservation.roomNumber()),
            body
        );
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(ReservationCancelled event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();
        String body = String.format("""
            A reserva do quarto %s foi cancelada.

            Período liberado:
            Entrada: %s
            Saída: %s""",
            reservation.roomNumber(),
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()));

        Message message = new Message(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            String.format("Reserva cancelada - quarto %s", reservation.roomNumber()),
            body
        );
        messageRepository.save(message);
    }
}
