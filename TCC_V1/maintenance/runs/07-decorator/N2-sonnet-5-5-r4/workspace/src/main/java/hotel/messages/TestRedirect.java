package hotel.messages;

/** In test mode nothing reaches the real address: the message goes to the test mailbox, saying who it was for. */
public final class TestRedirect implements MessageSender {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private final MessageSender next;

    public TestRedirect(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(TEST_ADDRESS, "[TESTE] " + subject, "Destinatário original: " + to + "\n" + body);
    }
}
