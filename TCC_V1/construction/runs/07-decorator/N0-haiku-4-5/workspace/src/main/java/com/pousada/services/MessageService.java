package com.pousada.services;

import com.pousada.models.Message;
import com.pousada.repositories.MessageRepository;
import com.pousada.utils.EmailValidator;

public class MessageService {
    private final MessageRepository messageRepository;
    private final boolean testMode;
    private final boolean archiveEnabled;

    public MessageService(MessageRepository messageRepository, boolean testMode, boolean archiveEnabled) {
        this.messageRepository = messageRepository;
        this.testMode = testMode;
        this.archiveEnabled = archiveEnabled;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, String comprovante) throws Exception {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + guestName + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     comprovante + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        sendMessage("EMAIL", guestEmail, subject, body);
    }

    public void sendMessage(String channel, String recipient, String subject, String body) throws Exception {
        String originalRecipient = recipient;

        if (!EmailValidator.isValid(originalRecipient)) {
            Message blockedMsg = new Message(channel, originalRecipient, subject, body, "BLOCKED");
            messageRepository.create(blockedMsg);
            return;
        }

        if (testMode) {
            String originalLine = "Destinatário original: " + originalRecipient + "\n";
            body = originalLine + body;
            recipient = "testes@pousadamarealta.com.br";
            subject = "[TESTE] " + subject;
        } else if (archiveEnabled) {
            Message archiveMsg = new Message(channel, "arquivo@pousadamarealta.com.br",
                                            "[Cópia] " + subject, body, "SENT");
            messageRepository.create(archiveMsg);
        }

        Message msg = new Message(channel, recipient, subject, body, "SENT");
        messageRepository.create(msg);
    }
}
