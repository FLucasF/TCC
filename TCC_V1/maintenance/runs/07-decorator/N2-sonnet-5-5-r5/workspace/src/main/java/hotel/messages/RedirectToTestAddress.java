package hotel.messages;

/** Test mode: the message goes to a test address, saying who would have received it. */
public final class RedirectToTestAddress implements MessageSender {

    private final MessageSender next;
    private final String testAddress;

    public RedirectToTestAddress(MessageSender next, String testAddress) {
        this.next = next;
        this.testAddress = testAddress;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(testAddress, "[TESTE] " + subject, "Destinatário original: " + to + "\n" + body);
    }
}
