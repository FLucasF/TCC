package br.com.marealta.mail;

import br.com.marealta.persistence.MessageRepository;

import java.sql.Connection;
import java.sql.SQLException;

/** Aplica as regras e guarda na lista de mensagens o que resultou. */
public final class Mailer {
    private final MailPipeline pipeline;
    private final MessageRepository messages;

    public Mailer(MailPipeline pipeline, MessageRepository messages) {
        this.pipeline = pipeline;
        this.messages = messages;
    }

    public void send(Connection connection, Message message) throws SQLException {
        for (Message resulting : pipeline.process(message)) {
            messages.insert(connection, resulting);
        }
    }
}
