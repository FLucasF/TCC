package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessor {
    private final boolean testMode;

    public MessageProcessor(boolean testMode) {
        this.testMode = testMode;
    }

    public List<ProcessedMessage> process(OutgoingMessage message) {
        if (!isValidEmail(message.getTo())) {
            return List.of(new ProcessedMessage(
                message.getChannel(),
                message.getTo(),
                message.getSubject(),
                message.getBody(),
                MessageStatus.BLOCKED
            ));
        }

        ProcessedMessage primaryMessage;
        List<ProcessedMessage> additionalMessages = new ArrayList<>();

        if (testMode) {
            String newSubject = "[TESTE] " + message.getSubject();
            String newBody = "Destinatário original: " + message.getTo() + "\n" + message.getBody();
            primaryMessage = new ProcessedMessage(
                message.getChannel(),
                "testes@pousadamarealta.com.br",
                newSubject,
                newBody,
                MessageStatus.SENT
            );
        } else {
            primaryMessage = new ProcessedMessage(
                message.getChannel(),
                message.getTo(),
                message.getSubject(),
                message.getBody(),
                MessageStatus.SENT
            );

            String archiveSubject = "[Cópia] " + message.getSubject();
            ProcessedMessage archiveCopy = new ProcessedMessage(
                message.getChannel(),
                "arquivo@pousadamarealta.com.br",
                archiveSubject,
                message.getBody(),
                MessageStatus.SENT
            );
            additionalMessages.add(archiveCopy);
        }

        List<ProcessedMessage> finalResults = new ArrayList<>();
        finalResults.add(primaryMessage);
        finalResults.addAll(additionalMessages);

        return finalResults;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
