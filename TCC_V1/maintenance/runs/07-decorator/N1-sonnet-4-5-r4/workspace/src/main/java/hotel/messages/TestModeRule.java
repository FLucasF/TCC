package hotel.messages;

final class TestModeRule implements EmailRule {

    @Override
    public void apply(OutgoingMessage message, RuleResult result) {
        if (message.status() == MessageStatus.BLOCKED) {
            return;
        }
        String originalRecipient = message.to();
        message.changeTo("testes@pousadamarealta.com.br");
        message.prependSubject("[TESTE] ");
        message.prependBody("Destinatário original: " + originalRecipient + "\n");
    }
}
