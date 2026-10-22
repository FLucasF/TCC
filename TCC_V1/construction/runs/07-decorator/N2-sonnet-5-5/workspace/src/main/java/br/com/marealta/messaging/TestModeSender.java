package br.com.marealta.messaging;

/** Modo de teste: nada sai para o endereço real; vai para o endereço de testes, com o original no texto. */
public final class TestModeSender implements MessageSender {
    private final String testAddress;
    private final MessageSender next;

    public TestModeSender(String testAddress, MessageSender next) {
        this.testAddress = testAddress;
        this.next = next;
    }

    @Override
    public Message send(Message message) {
        return next.send(message
                .withBody("Destinatário original: " + message.to() + "\n" + message.body())
                .withSubject("[TESTE] " + message.subject())
                .withTo(testAddress));
    }
}
