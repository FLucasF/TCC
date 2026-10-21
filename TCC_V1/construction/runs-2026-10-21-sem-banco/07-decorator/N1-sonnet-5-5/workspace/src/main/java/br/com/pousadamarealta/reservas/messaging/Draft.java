package br.com.pousadamarealta.reservas.messaging;

import java.util.ArrayList;
import java.util.List;

/** Mensagem em preparo: as regras podem alterá-la, bloqueá-la ou pedir cópias. */
public final class Draft {

    private final Channel channel;
    private String to;
    private String subject;
    private String body;
    private boolean blocked;
    private final List<Draft> copies = new ArrayList<>();

    public Draft(Channel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public void block() {
        blocked = true;
    }

    public void redirect(String newTo, String newSubject, String newBody) {
        to = newTo;
        subject = newSubject;
        body = newBody;
    }

    /** Pede uma mensagem adicional, enviada junto com esta. */
    public void addCopy(String copyTo, String copySubject, String copyBody) {
        copies.add(new Draft(channel, copyTo, copySubject, copyBody));
    }

    public Channel channel() { return channel; }
    public String to() { return to; }
    public String subject() { return subject; }
    public String body() { return body; }
    public boolean isBlocked() { return blocked; }
    public List<Draft> copies() { return copies; }
}
