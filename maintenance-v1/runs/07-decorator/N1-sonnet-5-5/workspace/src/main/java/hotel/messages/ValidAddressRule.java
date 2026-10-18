package hotel.messages;

import java.util.List;

/** Blocks e-mails to addresses like "ana@gmail" or "ana.gmail.com". */
final class ValidAddressRule implements EmailRule {

    @Override
    public List<Outgoing> apply(Outgoing email) {
        return List.of(isValid(email.to()) ? email : email.block());
    }

    private static boolean isValid(String address) {
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
