package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.messaging.MessageContext;
import com.pousadamarealta.messaging.MessageRequest;
import com.pousadamarealta.messaging.MessageRule;

public class TestModeRedirectRule implements MessageRule {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    @Override
    public MessageContext apply(MessageContext context) {
        MessageRequest current = context.getCurrent();
        String originalTo = current.getTo();

        String newSubject = TEST_PREFIX + current.getSubject();
        String newBody = "Destinatário original: " + originalTo + "\n" + current.getBody();

        MessageRequest redirected = current
                .withTo(TEST_EMAIL)
                .withSubject(newSubject)
                .withBody(newBody);

        return context.withCurrent(redirected);
    }
}
