package hotel.messages;

/** Blocks messages with invalid e-mail addresses. */
final class EmailValidationRule implements MessageRule {

    @Override
    public MessageToSend apply(MessageToSend message, boolean testMode) {
        if (!EmailValidator.isValid(message.to())) {
            return message.asBlocked();
        }
        return message;
    }
}
