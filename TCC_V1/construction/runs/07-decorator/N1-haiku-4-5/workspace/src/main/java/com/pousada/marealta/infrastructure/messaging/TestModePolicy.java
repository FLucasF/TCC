package com.pousada.marealta.infrastructure.messaging;

public class TestModePolicy implements MessagePolicy {
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final boolean testModeEnabled;

    public TestModePolicy(boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    @Override
    public MessagePolicyResult apply(MessageContext context) {
        if (!testModeEnabled) {
            return MessagePolicyResult.pass(context.originalRecipient, context.originalSubject, context.originalBody);
        }

        String newSubject = TEST_PREFIX + context.originalSubject;
        String newBody = "Destinatário original: " + context.originalRecipient + "\n\n" + context.originalBody;

        return MessagePolicyResult.pass(TEST_EMAIL, newSubject, newBody);
    }
}
