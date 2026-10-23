package com.pousada.messaging;

import com.pousada.domain.Message;

public class TestModePolicy implements MessagePolicy {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final boolean testMode;

    public TestModePolicy(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(PolicyResult result) {
        if (!testMode || result.isBlocked()) {
            return;
        }

        Message original = result.getMessage();
        String newBody = "Destinatário original: " + original.getTo() + "\n" + original.getBody();

        Message redirected = original.withTransformedContent(
            TEST_EMAIL,
            TEST_PREFIX + original.getSubject(),
            newBody
        );

        result.transform(redirected);
    }
}
