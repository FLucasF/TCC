package hotel.messages;

import java.util.List;

/** Blocks the message when the address is not like name@site.com, so the front desk can call the guest. */
public final class BlockInvalidAddress implements MessageRule {

    @Override
    public List<Draft> apply(Draft draft) {
        return List.of(isValid(draft.to()) ? draft : draft.blocked());
    }

    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.length() >= 3 && domain.substring(1, domain.length() - 1).contains(".");
    }
}
