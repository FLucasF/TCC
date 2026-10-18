package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** An e-mail on its way out, which the {@link MessageRule}s may redirect, rewrite, block or accompany with copies. */
public final class OutgoingEmail {

    private final String originalTo;
    private String to;
    private String subject;
    private String body;
    private boolean blocked;
    private final List<OutgoingEmail> copies = new ArrayList<>();

    public OutgoingEmail(String to, String subject, String body) {
        this.originalTo = to;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public String originalTo() {
        return originalTo;
    }

    public String to() {
        return to;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public boolean blocked() {
        return blocked;
    }

    List<OutgoingEmail> copies() {
        return copies;
    }

    public void block() {
        this.blocked = true;
    }

    public void redirectTo(String newTo) {
        this.to = newTo;
    }

    public void changeSubject(String newSubject) {
        this.subject = newSubject;
    }

    public void changeBody(String newBody) {
        this.body = newBody;
    }

    public void addCopy(String copyTo, String copySubject, String copyBody) {
        copies.add(new OutgoingEmail(copyTo, copySubject, copyBody));
    }
}
