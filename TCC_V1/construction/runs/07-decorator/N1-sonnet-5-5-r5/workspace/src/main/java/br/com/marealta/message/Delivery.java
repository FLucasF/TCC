package br.com.marealta.message;

import java.util.ArrayList;
import java.util.List;

/** A mensagem em preparo: cada regra pode bloquear, alterar ou acrescentar cópias. */
public class Delivery {

    private String to;
    private String subject;
    private String body;
    private boolean blocked;
    private final Channel channel;
    private final List<Message> copies = new ArrayList<>();

    public Delivery(Message draft) {
        this.channel = draft.channel();
        this.to = draft.to();
        this.subject = draft.subject();
        this.body = draft.body();
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

    public boolean isBlocked() {
        return blocked;
    }

    public void block() {
        blocked = true;
    }

    public void redirect(String newTo, String newSubject, String newBody) {
        this.to = newTo;
        this.subject = newSubject;
        this.body = newBody;
    }

    public void addCopy(String copyTo, String copySubject) {
        copies.add(new Message(null, channel, copyTo, copySubject, body, MessageStatus.SENT));
    }

    /** A mensagem em si, seguida das cópias, na ordem em que devem aparecer na lista. */
    public List<Message> result() {
        List<Message> all = new ArrayList<>();
        all.add(new Message(null, channel, to, subject, body, blocked ? MessageStatus.BLOCKED : MessageStatus.SENT));
        all.addAll(copies);
        return all;
    }
}
