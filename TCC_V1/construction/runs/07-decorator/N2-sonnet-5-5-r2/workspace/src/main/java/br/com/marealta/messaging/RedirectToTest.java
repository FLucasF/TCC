package br.com.marealta.messaging;

/** Modo de teste: nada sai para o endereço real. */
public class RedirectToTest implements MessageSender {
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private final MessageSender next;

    public RedirectToTest(MessageSender next) { this.next = next; }

    @Override
    public MessageStatus send(Message message) {
        return next.send(message
                .withTo(TEST_ADDRESS)
                .withSubject("[TESTE] " + message.subject())
                .withBody("Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
