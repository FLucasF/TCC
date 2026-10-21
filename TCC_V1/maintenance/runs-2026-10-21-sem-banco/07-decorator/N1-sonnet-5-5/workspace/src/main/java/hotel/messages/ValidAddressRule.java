package hotel.messages;

import java.util.List;

/** Blocks e-mails to addresses that cannot be right, so the front desk can call the guest. */
public final class ValidAddressRule implements MessageRule {

    @Override
    public List<Draft> apply(Draft draft) {
        return List.of(isValid(draft.to()) ? draft : draft.asBlocked());
    }

    /** Exactly one @, something before it, and after it a name with a dot in the middle. */
    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        return domain.indexOf('.') > 0 && lastDot < domain.length() - 1;
    }
}
