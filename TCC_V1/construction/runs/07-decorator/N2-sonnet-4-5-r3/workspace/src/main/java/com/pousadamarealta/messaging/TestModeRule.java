package com.pousadamarealta.messaging;

public final class TestModeRule implements MessageRule {
    private static final String TEST_RECIPIENT = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    @Override
    public void process(MessageContext context) {
        if (context.getStatus() == MessageStatus.BLOCKED) {
            return;
        }

        OutgoingMessage original = context.getMessage();
        String originalRecipient = original.to();

        OutgoingMessage modified = original
            .withTo(TEST_RECIPIENT)
            .prependToSubject(TEST_PREFIX)
            .prependToBody("Destinatário original: " + originalRecipient + "\n");

        context.setMessage(modified);
    }
}
