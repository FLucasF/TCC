package hotel.messages;

/** Test mode: the message never goes to the real address; it goes to the test address, saying who was the target. */
final class RedirectToTests implements MessageSender {

    private final MessageSender next;
    private final String testAddress;

    RedirectToTests(MessageSender next, String testAddress) {
        this.next = next;
        this.testAddress = testAddress;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return next.send(testAddress, "[TESTE] " + subject, "Destinatário original: " + to + "\n" + body);
    }
}
