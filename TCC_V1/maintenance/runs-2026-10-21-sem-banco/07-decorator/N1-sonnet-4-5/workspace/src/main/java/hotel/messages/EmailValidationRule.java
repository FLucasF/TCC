package hotel.messages;

/** Blocks messages with invalid e-mail addresses. */
final class EmailValidationRule implements EmailRule {

    @Override
    public void apply(OutboundMessage message) {
        if (!isValid(message.to())) {
            message.block();
        }
    }

    private static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
