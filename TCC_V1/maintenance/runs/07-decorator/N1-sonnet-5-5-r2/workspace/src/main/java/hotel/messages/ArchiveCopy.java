package hotel.messages;

import java.util.List;

/** Keeps a copy of every message sent, for the accountant. */
final class ArchiveCopy implements MessageRule {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<Draft> extrasFor(Draft sent) {
        return List.of(new Draft(ARCHIVE_ADDRESS, "[Cópia] " + sent.subject(), sent.body()));
    }
}
