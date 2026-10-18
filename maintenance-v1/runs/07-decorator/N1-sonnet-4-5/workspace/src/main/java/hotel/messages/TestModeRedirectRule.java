package hotel.messages;

/** In test mode, redirects messages to the test address and adds the original recipient to the body. */
final class TestModeRedirectRule implements MessageRule {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[TESTE] ";

    @Override
    public MessageToSend apply(MessageToSend message, boolean testMode) {
        if (!testMode || message.status() == MessageStatus.BLOCKED) {
            return message;
        }

        String newSubject = SUBJECT_PREFIX + message.subject();
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();
        return message.withModified(TEST_ADDRESS, newSubject, newBody);
    }
}
