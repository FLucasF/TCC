package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;

public final class TestModeRule implements MessageRule {
    private static final String TEST_RECIPIENT = "testes@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[TESTE] ";

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        String newSubject = SUBJECT_PREFIX + message.subject();
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();

        OutgoingMessage modifiedMessage = new OutgoingMessage(
            message.channel(),
            TEST_RECIPIENT,
            newSubject,
            newBody
        );

        return ProcessingResult.proceed(modifiedMessage);
    }
}
