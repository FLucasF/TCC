package hotel.messages;

/**
 * In test mode, redirects messages to a test address instead of the real recipient, so guests
 * never get e-mails sent while the system is being tested.
 */
public final class TestModeFilter implements EmailFilter {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public FilterResult apply(OutgoingMessage message) {
        String newSubject = "[TESTE] " + message.subject();
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();
        OutgoingMessage redirected = new OutgoingMessage(TEST_ADDRESS, newSubject, newBody);
        return FilterResult.allow(redirected);
    }
}
