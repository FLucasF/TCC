package br.com.pousadamarealta.events.listeners;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.events.ReservationCancelledEvent;
import br.com.pousadamarealta.events.ReservationConfirmedEvent;
import br.com.pousadamarealta.events.ReservationEvent;
import br.com.pousadamarealta.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;

public class HousekeepingNotifier implements ReservationEventListener {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public HousekeepingNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            handleConfirmed(confirmed);
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            handleCancelled(cancelled);
        }
    }

    private void handleConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = buildConfirmedBody(reservation);

        Message message = new Message("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private void handleCancelled(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.getReservation();
        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = buildCancelledBody(reservation);

        Message message = new Message("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmedBody(Reservation reservation) {
        return String.format(
            "Entrada: %s | Saída: %s | %d %s",
            reservation.formatDate(reservation.getCheckIn()),
            reservation.formatDate(reservation.getCheckOut()),
            reservation.getGuests(),
            reservation.getGuests() == 1 ? "hóspede" : "hóspedes"
        );
    }

    private String buildCancelledBody(Reservation reservation) {
        return String.format(
            "Datas livres: %s a %s",
            reservation.formatDate(reservation.getCheckIn()),
            reservation.formatDate(reservation.getCheckOut())
        );
    }
}
