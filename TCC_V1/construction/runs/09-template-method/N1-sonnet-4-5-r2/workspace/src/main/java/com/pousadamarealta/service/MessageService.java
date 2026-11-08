package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final DocumentService documentService;

    public MessageService(MessageRepository messageRepository,
                         RoomRepository roomRepository,
                         DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.documentService = documentService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String receipt = documentService.generateReceipt(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            receipt);

        Message message = new Message(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
