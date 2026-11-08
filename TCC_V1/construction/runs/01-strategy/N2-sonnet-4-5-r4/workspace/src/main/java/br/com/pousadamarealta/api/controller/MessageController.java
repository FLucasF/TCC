package br.com.pousadamarealta.api.controller;

import br.com.pousadamarealta.api.dto.MessageResponse;
import br.com.pousadamarealta.domain.repository.MessageRepository;
import br.com.pousadamarealta.infrastructure.database.DatabaseConfig;
import io.javalin.http.Context;

import java.sql.Connection;
import java.util.List;

public class MessageController {
    private final DatabaseConfig databaseConfig;

    public MessageController(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void getMessages(Context ctx) {
        try (Connection connection = databaseConfig.getConnection()) {
            MessageRepository messageRepository = new MessageRepository(connection);
            List<MessageResponse> messages = messageRepository.findAll().stream()
                .map(MessageResponse::from)
                .toList();
            ctx.json(messages);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }
}
