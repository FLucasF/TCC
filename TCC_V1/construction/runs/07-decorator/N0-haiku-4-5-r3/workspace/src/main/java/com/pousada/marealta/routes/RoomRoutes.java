package com.pousada.marealta.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.marealta.service.RoomService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class RoomRoutes {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void getAllRooms(Context ctx) {
        try {
            var rooms = RoomService.getAllRooms();
            ctx.json(rooms);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(errorResponse("INTERNAL_ERROR", "Database error"));
        }
    }

    private static Map<String, Object> errorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
