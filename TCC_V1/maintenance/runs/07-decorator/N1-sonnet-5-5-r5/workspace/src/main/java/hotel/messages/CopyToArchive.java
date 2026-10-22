package hotel.messages;

/** The accountant keeps a copy of everything the inn sends. */
public final class CopyToArchive implements MessageRule {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public Draft apply(Draft draft) {
        return draft.withCopy(new Draft(ARCHIVE_ADDRESS, "[Cópia] " + draft.subject(), draft.body()));
    }
}
