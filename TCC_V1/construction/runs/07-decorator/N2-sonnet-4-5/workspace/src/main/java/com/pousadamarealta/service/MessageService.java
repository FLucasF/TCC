package com.pousadamarealta.service;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> sentMessages = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final MessageProcessor processor;

    public MessageService(MessageProcessor processor) {
        this.processor = processor;
    }

    public void sendConfirmationEmail(String to, String guestName, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationBody(guestName, document);
        sendEmail(to, subject, body);
    }

    private String buildConfirmationBody(String guestName, Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(document.format()).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private void sendEmail(String to, String subject, String body) {
        Message message = new Message(nextId.getAndIncrement(), "EMAIL", to, subject, body, MessageStatus.SENT);
        List<Message> processed = processor.process(List.of(message));

        synchronized (sentMessages) {
            sentMessages.addAll(processed);

            long maxId = processed.stream()
                .mapToLong(Message::id)
                .max()
                .orElse(nextId.get() - 1);

            if (maxId >= nextId.get()) {
                nextId.set(maxId + 1);
            }
        }
    }

    public List<Message> getAllMessages() {
        synchronized (sentMessages) {
            return new ArrayList<>(sentMessages);
        }
    }
}
