package hotel.messages;

final class ArchiveCopyRule implements EmailRule {

    @Override
    public void apply(OutgoingMessage message, RuleResult result) {
        if (message.status() == MessageStatus.BLOCKED) {
            return;
        }
        result.addMessage("arquivo@pousadamarealta.com.br", "[Cópia] " + message.subject(), message.body());
    }
}
