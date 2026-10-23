package hotel.messages;

/**
 * Blocks messages sent to invalid e-mail addresses, so the front desk can call the guest instead.
 * Valid means one @, something before it, and a name with at least one dot after it.
 */
public final class EmailValidator implements EmailFilter {

    @Override
    public FilterResult apply(OutgoingMessage message) {
        if (isValid(message.to())) {
            return FilterResult.allow(message);
        }
        return FilterResult.block(message);
    }

    private boolean isValid(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex == -1 || atIndex != email.lastIndexOf('@')) {
            return false; // no @ or more than one
        }
        if (atIndex == 0) {
            return false; // nothing before @
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }
}
