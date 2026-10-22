package hotel.messages;

public final class EmailValidationRule implements EmailRule {

    @Override
    public void apply(ProcessingResult result) {
        OutgoingMessage message = result.original();
        if (!EmailValidator.isValid(message.to())) {
            result.setOriginal(message.withStatus(MessageStatus.BLOCKED));
        }
    }
}
