package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.messaging.MessagePipeline;
import com.pousada.marealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessagePipeline messagePipeline;

    public MessageService(MessageRepository messageRepository, MessagePipeline messagePipeline) {
        this.messageRepository = messageRepository;
        this.messagePipeline = messagePipeline;
    }

    /**
     * Envia uma mensagem processando-a através do pipeline de regras.
     * O pipeline pode bloquear, redirecionar ou criar cópias da mensagem.
     */
    public void sendMessage(String to, String subject, String body) {
        Message message = new Message(to, subject, body);

        // Processa a mensagem através do pipeline de regras
        List<Message> processedMessages = messagePipeline.process(message);

        // Salva todas as mensagens geradas (original + cópias)
        for (Message msg : processedMessages) {
            messageRepository.save(msg);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
