package br.com.marealta.message;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Por enquanto nada sai de verdade: as mensagens ficam guardadas para a recepção consultar. */
public class MessageDispatcher {

    private final MessagePipeline pipeline;
    private final MessageRepository repository;

    public MessageDispatcher(MessagePipeline pipeline, MessageRepository repository) {
        this.pipeline = pipeline;
        this.repository = repository;
    }

    public void send(Connection connection, Message draft) throws SQLException {
        // Serializa o envio para a cópia ficar logo depois da original na lista.
        try (Statement statement = connection.createStatement()) {
            statement.execute("select pg_advisory_xact_lock(7071)");
        }
        for (Message message : pipeline.process(draft)) {
            repository.insert(connection, message);
        }
    }
}
