package com.pousada;

import com.pousada.api.ApiHandler;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        int port = getPort();

        Javalin app = Javalin.create()
            .start(port);

        ApiHandler apiHandler = new ApiHandler();
        apiHandler.registerRoutes(app);

        System.out.println("Pousada Maré Alta API running on port " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable, using default 7070");
            }
        }
        return 7070;
    }
}
