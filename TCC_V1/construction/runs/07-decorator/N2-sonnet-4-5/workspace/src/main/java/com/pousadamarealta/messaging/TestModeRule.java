package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;

import java.util.List;

public class TestModeRule implements MessageRule {
    private final String testEmail;

    public TestModeRule(String testEmail) {
        this.testEmail = testEmail;
    }

    @Override
    public List<Message> process(List<Message> messages) {
        return messages.stream()
            .map(this::redirectToTest)
            .toList();
    }

    private Message redirectToTest(Message message) {
        String newSubject = "[TESTE] " + message.subject();
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();
        return message.withModifications(testEmail, newSubject, newBody);
    }
}
