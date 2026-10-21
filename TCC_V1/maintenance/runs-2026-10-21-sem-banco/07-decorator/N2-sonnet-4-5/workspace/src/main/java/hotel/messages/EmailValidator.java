package hotel.messages;

/**
 * Validates the e-mail address. An invalid address causes the message to be blocked: it will not be
 * sent and will appear in the list as BLOCKED, so the front desk can call the guest.
 *
 * <p>A valid address has exactly one @, something before it, and after it a name with at least one dot
 * (like ana@gmail.com).
 */
final class EmailValidator implements EmailHandler {

    @Override
    public boolean handle(EmailContext context) {
        if (!isValid(context.to)) {
            context.status = MessageStatus.BLOCKED;
            return false;
        }
        return true;
    }

    private static boolean isValid(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        return !localPart.isEmpty() && domainPart.contains(".");
    }
}
