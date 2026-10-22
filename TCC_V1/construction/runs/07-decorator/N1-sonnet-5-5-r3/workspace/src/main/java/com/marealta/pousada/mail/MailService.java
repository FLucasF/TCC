package com.marealta.pousada.mail;

import com.marealta.pousada.persistence.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public final class MailService {
    private final MessageRepository repository;
    private final List<MailRule> rules;

    public MailService(MessageRepository repository, List<ScopedRule> rules, boolean testMode) {
        this.repository = repository;
        this.rules = rules.stream().filter(r -> r.scope().appliesTo(testMode)).map(ScopedRule::rule).toList();
    }

    public synchronized void sendEmail(String to, String subject, String body) {
        List<Message> outgoing = List.of(Message.email(to, subject, body));
        for (MailRule rule : rules) {
            List<Message> next = new ArrayList<>();
            for (Message message : outgoing) {
                if (message.isBlocked()) {
                    next.add(message);
                } else {
                    next.addAll(rule.apply(message));
                }
            }
            outgoing = next;
        }
        outgoing.forEach(repository::save);
    }

    public List<Message> sent() {
        return repository.findAll();
    }
}
