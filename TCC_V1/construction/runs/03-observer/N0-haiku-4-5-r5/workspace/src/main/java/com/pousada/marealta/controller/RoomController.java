package com.pousada.marealta.controller;

import com.pousada.marealta.repository.RoomRepository;
import io.javalin.Javalin;

import java.sql.SQLException;

public class RoomController {
    public static void register(Javalin app) {
        app.get("/rooms", ctx -> {
            try {
                var rooms = new RoomRepository().findAll();
                ctx.json(rooms.stream().map(r -> new Object() {
                    public final int number = r.getNumber();
                    public final String type = r.getType().name();
                    public final int capacity = r.getCapacity();
                    public final Object nightlyRate = r.getNightlyRate();
                }).toList());
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(new Object() {
                    public final String error = "DATABASE_ERROR";
                    public final String message = e.getMessage();
                });
            }
        });
    }
}
