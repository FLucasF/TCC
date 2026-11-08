package br.com.pousadamarealta.domain.service;

import br.com.pousadamarealta.domain.model.Message;
import br.com.pousadamarealta.domain.model.MessageChannel;
import br.com.pousadamarealta.domain.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(MessageChannel.EMAIL, to, subject, body);
        messageRepository.save(message);
    }
}
