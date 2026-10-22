package hotel.messages;

/** Validates email addresses before sending. Invalid addresses result in blocked messages. */
final class EmailValidationDecorator implements MessageSender {

    private final MessageSender next;
    private final SentMessages sentMessages;

    EmailValidationDecorator(MessageSender next, SentMessages sentMessages) {
        this.next = next;
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        if (!isValidEmail(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }
        return next.send(to, subject, body);
    }

    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
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
