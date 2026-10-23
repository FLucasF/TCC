package com.pousada.service.messaging;

public class TestModeRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        if (message.isBlocked()) {
            return new ProcessingResult();
        }

        String originalTo = message.getTo();
        message.setTo(TEST_EMAIL);
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + originalTo + "\n" + message.getBody());

        return new ProcessingResult();
    }
}
