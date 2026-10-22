package com.posadamareaita.service;

import com.posadamareaita.domain.Message;
import com.posadamareaita.repository.MessageRepository;

public class MessageService {
    public static void sendEmail(String to, String subject, String body) throws Exception {
        Message message = new Message("EMAIL", to, subject, body);
        MessageRepository.save(message);
    }
}
