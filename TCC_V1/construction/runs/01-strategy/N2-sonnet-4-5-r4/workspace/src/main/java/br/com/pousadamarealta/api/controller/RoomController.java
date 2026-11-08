package br.com.pousadamarealta.api.controller;

import br.com.pousadamarealta.api.dto.RoomResponse;
import br.com.pousadamarealta.domain.repository.RoomRepository;
import br.com.pousadamarealta.infrastructure.database.DatabaseConfig;
import io.javalin.http.Context;

import java.sql.Connection;
import java.util.List;

public class RoomController {
    private final DatabaseConfig databaseConfig;

    public RoomController(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void getRooms(Context ctx) {
        try (Connection connection = databaseConfig.getConnection()) {
            RoomRepository roomRepository = new RoomRepository(connection);
            List<RoomResponse> rooms = roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
            ctx.json(rooms);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar requisição", e);
        }
    }
}
