package br.com.marealta.messaging;

/** Sends the message to a test address instead of the real one, saying who it was meant for. */
public final class TestRedirect implements MessageSink {
    private static final String SUBJECT_PREFIX = "[TESTE] ";

    private final String testAddress;
    private final MessageSink next;

    public TestRedirect(String testAddress, MessageSink next) {
        this.testAddress = testAddress;
        this.next = next;
    }

    @Override
    public void deliver(Message message) {
        next.deliver(message.with(testAddress, SUBJECT_PREFIX + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
