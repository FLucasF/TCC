package com.pousadamarealta.messaging;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ArchiveCopyHandler implements MessageHandler {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private final boolean testMode;
    private final AtomicLong idGenerator;

    public ArchiveCopyHandler(boolean testMode, AtomicLong idGenerator) {
        this.testMode = testMode;
        this.idGenerator = idGenerator;
    }

    @Override
    public void handle(OutgoingMessage message, List<Message> sentMessages) {
        // Não criar cópia em modo de teste nem para mensagens bloqueadas
        if (testMode || message.isBlocked()) {
            return;
        }

        // Criar cópia para o arquivo
        Message archiveCopy = new Message(
                idGenerator.incrementAndGet(),
                MessageChannel.EMAIL,
                ARCHIVE_EMAIL,
                "[Cópia] " + message.getSubject(),
                message.getBody(),
                MessageStatus.SENT
        );

        sentMessages.add(archiveCopy);
    }
}
