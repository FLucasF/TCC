package hotel.messages;

/** Test mode: nothing goes to the real address; it goes to the test mailbox, saying who it was meant for. */
public final class RedirectToTestAddress implements MessageSender {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private final MessageSender next;

    public RedirectToTestAddress(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(TEST_ADDRESS, "[TESTE] " + subject, "Destinatário original: " + to + "\n" + body);
    }
}
