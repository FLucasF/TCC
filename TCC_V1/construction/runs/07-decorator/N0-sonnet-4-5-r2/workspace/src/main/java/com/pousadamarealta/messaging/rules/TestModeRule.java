package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.ProcessingContext;
import com.pousadamarealta.messaging.ProcessingResult;

public class TestModeRule implements MessageProcessor {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    @Override
    public ProcessingResult process(Message message, ProcessingContext context) {
        if (context.isTestMode()) {
            String originalRecipient = message.getTo();
            String originalBody = message.getBody();

            message.setTo(TEST_EMAIL);
            message.setSubject(TEST_PREFIX + message.getSubject());
            message.setBody("Destinatário original: " + originalRecipient + "\n" + originalBody);
        }

        return ProcessingResult.continueProcessing(message);
    }
}
