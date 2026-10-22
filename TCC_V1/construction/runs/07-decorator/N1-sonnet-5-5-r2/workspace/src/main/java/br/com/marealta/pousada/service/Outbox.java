package br.com.marealta.pousada.service;

import br.com.marealta.pousada.mail.MailPipeline;
import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.store.Database;
import br.com.marealta.pousada.store.MessageRepository;
import java.sql.Statement;
import java.util.List;

/** A lista de mensagens enviadas: aplica as regras de e-mail e registra o resultado. */
public class Outbox {
    private final Database database;
    private final MessageRepository messages;
    private final MailPipeline pipeline;

    public Outbox(Database database, MessageRepository messages, MailPipeline pipeline) {
        this.database = database;
        this.messages = messages;
        this.pipeline = pipeline;
    }

    public void send(Message message) {
        List<Message> outgoing = pipeline.process(message);
        database.inTransaction(c -> {
            try (Statement s = c.createStatement()) {
                // mantém a mensagem e a sua cópia lado a lado na lista, mesmo com envios simultâneos
                s.execute("SELECT pg_advisory_xact_lock(727002)");
            }
            for (Message m : outgoing) {
                messages.insert(c, m);
            }
            return null;
        });
    }

    public List<Message> list() {
        return database.inTransaction(messages::findAll);
    }
}
