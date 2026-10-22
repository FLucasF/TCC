package com.pousadamaralta.domain.service;

import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.model.Channel;
import com.pousadamaralta.domain.model.Message;
import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.infrastructure.repository.MessageRepository;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - Pousada Maré Alta";

        GuestIdentification id = reservation.getIdentification();
        String document = id.generateDocument(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        messageRepository.save(Channel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
