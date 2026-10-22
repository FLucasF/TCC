package hotel.messages;

/** Validates email addresses and blocks messages to invalid addresses. */
public final class EmailValidationHandler implements MessageHandler {

    @Override
    public ProcessedMessage handle(OutgoingMessage message) {
        if (isValidEmail(message.to())) {
            return ProcessedMessage.sent(message.to(), message.subject(), message.body());
        } else {
            return ProcessedMessage.blocked(message.to());
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        int atIndex = email.indexOf('@');
        int lastAtIndex = email.lastIndexOf('@');

        if (atIndex == -1 || atIndex != lastAtIndex) {
            return false;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        if (localPart.isBlank() || domainPart.isBlank()) {
            return false;
        }

        return domainPart.contains(".");
    }
}
