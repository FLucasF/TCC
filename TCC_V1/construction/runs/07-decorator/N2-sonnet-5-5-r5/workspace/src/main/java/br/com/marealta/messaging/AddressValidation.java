package br.com.marealta.messaging;

/** Blocks messages whose address is not valid: one @, something before it, a dotted name after it. */
public final class AddressValidation implements MessageSink {
    private final MessageSink next;
    private final MessageSink blocked;

    public AddressValidation(MessageSink next, MessageSink blocked) {
        this.next = next;
        this.blocked = blocked;
    }

    @Override
    public void deliver(Message message) {
        if (isValid(message.to())) {
            next.deliver(message);
        } else {
            blocked.deliver(message.blocked());
        }
    }

    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && !domain.endsWith(".") && domain.chars().noneMatch(Character::isWhitespace);
    }
}
