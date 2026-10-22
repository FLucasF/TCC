package hotel.messages;

import java.util.List;

/** Every message sent gets a copy for the accountant, right after it. */
public final class CopyToArchive implements MessageRule {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<Draft> apply(Draft draft) {
        Draft copy = Draft.email(ARCHIVE_ADDRESS, "[Cópia] " + draft.subject(), draft.body());
        return List.of(draft, copy);
    }
}
