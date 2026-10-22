package hotel.messages;

/** Blocks a message whose address is not like ana@gmail.com: one @, something before it and a dotted name after. */
final class ValidAddress implements MessageRule {

    @Override
    public Draft apply(Draft draft) {
        return isValid(draft.to()) ? draft : draft.blocked();
    }

    static boolean isValid(String address) {
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.indexOf('.', 1) > 0 && !domain.startsWith(".") && !domain.endsWith(".");
    }
}
