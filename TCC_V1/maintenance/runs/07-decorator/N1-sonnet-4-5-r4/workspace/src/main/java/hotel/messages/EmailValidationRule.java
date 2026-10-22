package hotel.messages;

final class EmailValidationRule implements EmailRule {

    @Override
    public void apply(OutgoingMessage message, RuleResult result) {
        if (!isValid(message.to())) {
            message.block();
        }
    }

    private boolean isValid(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }
        if (atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
