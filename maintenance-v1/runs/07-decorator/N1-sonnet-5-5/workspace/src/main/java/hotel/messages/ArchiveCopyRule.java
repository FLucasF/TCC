package hotel.messages;

import java.util.List;

/** The accountant keeps a copy of everything the inn sends. */
final class ArchiveCopyRule implements EmailRule {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<Outgoing> apply(Outgoing email) {
        return List.of(
                email, new Outgoing(ARCHIVE_ADDRESS, "[Cópia] " + email.subject(), email.body(), false));
    }
}
