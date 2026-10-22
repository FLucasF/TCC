package hotel.messages;

import java.util.List;

/** A message to an address that cannot be right is blocked, so the front desk calls the guest. */
public final class ValidAddressRule implements MessageRule {

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        return List.of(isValid(message.to()) ? message : message.blocked());
    }

    /** One @, something before it, and after it a name with a dot in the middle. */
    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.length() > 2 && domain.substring(1, domain.length() - 1).contains(".");
    }
}
