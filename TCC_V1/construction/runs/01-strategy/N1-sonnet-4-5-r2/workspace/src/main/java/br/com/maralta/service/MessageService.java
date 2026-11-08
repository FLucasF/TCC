package br.com.maralta.service;

import br.com.maralta.domain.Message;
import br.com.maralta.domain.MessageChannel;
import br.com.maralta.repository.MessageRepository;

import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(MessageChannel.EMAIL, to, subject, body);
        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
