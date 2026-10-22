package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** An e-mail on its way out, while the rules look at it: they may change it, block it or add copies after it. */
public record Draft(String to, String subject, String body, boolean blocked, List<Draft> copies) {

    public Draft(String to, String subject, String body) {
        this(to, subject, body, false, List.of());
    }

    public Draft withTo(String newTo) {
        return new Draft(newTo, subject, body, blocked, copies);
    }

    public Draft withSubject(String newSubject) {
        return new Draft(to, newSubject, body, blocked, copies);
    }

    public Draft withBody(String newBody) {
        return new Draft(to, subject, newBody, blocked, copies);
    }

    public Draft block() {
        return new Draft(to, subject, body, true, copies);
    }

    public Draft withCopy(Draft copy) {
        List<Draft> all = new ArrayList<>(copies);
        all.add(copy);
        return new Draft(to, subject, body, blocked, List.copyOf(all));
    }
}
