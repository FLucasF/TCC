package hotel.messages;

/** Redirects messages to a test address when in test mode. */
final class TestModeDecorator implements MessageSender {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final MessageSender next;

    TestModeDecorator(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        String testSubject = "[TESTE] " + subject;
        String testBody = "Destinatário original: " + to + "\n" + body;
        return next.send(TEST_EMAIL, testSubject, testBody);
    }
}
