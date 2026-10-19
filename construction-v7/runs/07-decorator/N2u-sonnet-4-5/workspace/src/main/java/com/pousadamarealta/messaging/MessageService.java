package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MessageService {
    private final List<Message> sentMessages;
    private final AtomicLong idGenerator;
    private final List<MessageHandler> handlers;

    public MessageService(boolean testMode) {
        this.sentMessages = new ArrayList<>();
        this.idGenerator = new AtomicLong(0);

        // Montar a cadeia de handlers
        this.handlers = new ArrayList<>();
        this.handlers.add(new EmailValidationHandler());
        this.handlers.add(new TestModeHandler(testMode));
        this.handlers.add(new ArchiveCopyHandler(testMode, idGenerator));
    }

    public void send(String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(to, subject, body);
        List<Message> additionalMessages = new ArrayList<>();

        // Processar através da cadeia de handlers (exceto ArchiveCopyHandler)
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).handle(outgoing, sentMessages);
        }

        // Criar a mensagem final
        MessageStatus status = outgoing.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
        Message message = new Message(
                idGenerator.incrementAndGet(),
                MessageChannel.EMAIL,
                outgoing.getTo(),
                outgoing.getSubject(),
                outgoing.getBody(),
                status
        );

        sentMessages.add(message);

        // Processar o ArchiveCopyHandler por último (depois de adicionar a mensagem original)
        handlers.get(handlers.size() - 1).handle(outgoing, sentMessages);
    }

    public List<Message> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }
}
