package br.com.mareaalta.service;

import br.com.mareaalta.domain.DocumentData;
import br.com.mareaalta.domain.Message;
import br.com.mareaalta.domain.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public void sendConfirmationEmail(Reservation reservation, DocumentData document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation, document);

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        message.setId(idGenerator.getAndIncrement());
        messages.add(message);
    }

    private String buildConfirmationEmail(Reservation reservation, DocumentData document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(document.getText()).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
}
