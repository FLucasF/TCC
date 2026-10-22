package pousada.messaging;

/** Em modo de teste nada sai para o endereço real: tudo vai para o endereço de testes. */
public final class TestRedirect implements MessageSink {
    private final MessageSink next;
    private final String testAddress;

    public TestRedirect(MessageSink next, String testAddress) {
        this.next = next;
        this.testAddress = testAddress;
    }

    @Override
    public Message send(Message message) {
        return next.send(message
                .withTo(testAddress)
                .withSubject("[TESTE] " + message.subject())
                .withBody("Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
