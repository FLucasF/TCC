package hotel.messages;

import java.util.regex.Pattern;

/** A message to a mistyped address is not sent: it is blocked, for the front desk to call the guest. */
public final class BlockInvalidAddress implements MessageRule {

    // a single @, something before it, and after it a name with at least one dot in the middle
    private static final Pattern VALID = Pattern.compile("[^@]+@[^@.]+(\\.[^@.]+)+");

    @Override
    public Draft apply(Draft draft) {
        return VALID.matcher(draft.to()).matches() ? draft : draft.block();
    }
}
