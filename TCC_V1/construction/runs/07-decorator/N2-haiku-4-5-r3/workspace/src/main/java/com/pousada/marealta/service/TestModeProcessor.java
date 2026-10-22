package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;

public class TestModeProcessor implements MessageProcessor {
    private MessageProcessor nextProcessor;
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    public void setNext(MessageProcessor next) {
        this.nextProcessor = next;
    }

    @Override
    public ProcessingResult process(Message message, MessageProcessingContext context) {
        if (context.isTestMode()) {
            String originalTo = message.getTo();
            message.setTo(TEST_EMAIL);
            message.setSubject("[TESTE] " + message.getSubject());
            message.setBody("Destinatário original: " + originalTo + "\n\n" + message.getBody());
        }

        if (nextProcessor != null) {
            return nextProcessor.process(message, context);
        }

        return new ProcessingResult(ProcessingResult.Status.COMPLETED, message);
    }
}
