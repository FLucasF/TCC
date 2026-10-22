package com.pousadamarealta.events;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationListener implements
        EventListener<ReservationConfirmedEvent> {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    public HousekeepingNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();

        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
                "Entrada: %s | Saída: %s | %d %s",
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                reservation.getGuests(),
                reservation.getGuests() == 1 ? "hóspede" : "hóspedes"
        );

        Message message = new Message("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
        messageRepository.save(message);
    }
}
