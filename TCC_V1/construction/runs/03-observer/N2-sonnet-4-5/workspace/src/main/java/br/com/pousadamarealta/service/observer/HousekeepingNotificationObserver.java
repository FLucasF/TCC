package br.com.pousadamarealta.service.observer;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.repository.MessageRepository;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationObserver implements ReservationObserver {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public HousekeepingNotificationObserver(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String body = String.format("""
            Reserva confirmada para o quarto %s.
            Entrada: %s
            Saída: %s
            Hóspedes: %d""",
            reservation.getRoomNumber(),
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests());

        Message message = new Message(
            "EMAIL",
            "governanca@pousadamarealta.com.br",
            String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber()),
            body
        );
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }

        String body = String.format("""
            Reserva cancelada para o quarto %s.
            Datas liberadas:
            Entrada: %s
            Saída: %s""",
            reservation.getRoomNumber(),
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER));

        Message message = new Message(
            "EMAIL",
            "governanca@pousadamarealta.com.br",
            String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber()),
            body
        );
        messageRepository.save(message);
    }
}
