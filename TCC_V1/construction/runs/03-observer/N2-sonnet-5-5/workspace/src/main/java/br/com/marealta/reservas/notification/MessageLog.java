package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.persistence.MessageRepository;

/** Lista de mensagens enviadas. Por enquanto o e-mail não sai de verdade: só fica registrado aqui. */
public class MessageLog {
    private static final String EMAIL = "EMAIL";

    private final MessageRepository repository;

    public MessageLog(MessageRepository repository) {
        this.repository = repository;
    }

    public void sendEmail(String to, String subject, String body) {
        repository.insert(EMAIL, to, subject, body);
    }
}
