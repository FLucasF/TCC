package com.pousada;

import com.pousada.api.MessageEndpoints;
import com.pousada.api.ReportEndpoints;
import com.pousada.api.ReservationEndpoints;
import com.pousada.api.RoomEndpoints;
import com.pousada.db.Database;
import io.javalin.Javalin;
import java.sql.SQLException;

public class Application {
    public static void main(String[] args) {
        try {
            Database.initialize();

            Javalin app = Javalin.create();

            RoomEndpoints.register(app);
            ReservationEndpoints.register(app);
            MessageEndpoints.register(app);
            ReportEndpoints.register(app);

            int port = getPort();
            app.start(port);
            System.out.println("Pousada Maré Alta reservation system running on port " + port);

        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT value, using default 7070");
            }
        }
        return 7070;
    }
}
