package hotel.messages;

final class EmailValidationRule implements MessageRule {

    @Override
    public void apply(ProcessedMessage message) {
        if (!isValidEmail(message.to())) {
            message.setStatus(MessageStatus.BLOCKED);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }

        int atIndex = email.indexOf('@');
        if (atIndex == 0) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        if (domain.isEmpty() || !domain.contains(".")) {
            return false;
        }

        return true;
    }
}
