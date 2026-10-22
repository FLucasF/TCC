package hotel.messages;

/** Blocks messages with invalid e-mail addresses. */
final class EmailValidationRule implements MessageRule {

    @Override
    public void apply(MessageContext context) {
        if (!isValidEmail(context.message().to())) {
            context.block();
        }
    }

    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        long atCount = email.chars().filter(c -> c == '@').count();
        if (atCount != 1) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex == 0) {
            return false;
        }
        String afterAt = email.substring(atIndex + 1);
        return afterAt.contains(".");
    }
}
