package com.marealta.mail;

/** Blocks messages to malformed addresses so the front desk can call the guest. */
public final class AddressValidation implements Outbox {
    private final Outbox next;

    public AddressValidation(Outbox next) {
        this.next = next;
    }

    @Override
    public Message send(Message message) {
        return isValid(message.to()) ? next.send(message) : next.send(message.blocked());
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
        for (int i = 1; i < domain.length() - 1; i++) {
            if (domain.charAt(i) == '.') {
                return true;
            }
        }
        return false;
    }
}
