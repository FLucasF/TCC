package br.com.marealta;

/** Modo de teste: nada sai para o endereço real. */
public class RedirectToTestAddress implements MessageSender {
    private final MessageSender next;
    private final String testAddress;

    public RedirectToTestAddress(MessageSender next, String testAddress) {
        this.next = next;
        this.testAddress = testAddress;
    }

    @Override
    public void send(OutgoingMessage message) {
        next.send(message.withTo(testAddress)
                .withSubject("[TESTE] " + message.subject())
                .withBody("Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
