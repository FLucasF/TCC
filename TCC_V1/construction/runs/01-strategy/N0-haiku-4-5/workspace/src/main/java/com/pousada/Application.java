package com.pousada;

import com.pousada.db.Database;
import com.pousada.api.ReservationAPI;
import com.pousada.api.RoomAPI;
import com.pousada.api.ReportAPI;
import com.pousada.api.MessageAPI;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        Database.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create().start(port);

        RoomAPI.register(app);
        ReservationAPI.register(app);
        ReportAPI.register(app);
        MessageAPI.register(app);
    }
}
