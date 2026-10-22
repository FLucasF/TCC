package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.api.HotelAPI;

public class Main {
    public static void main(String[] args) {
        Database.initialize();

        Javalin app = Javalin.create().start(getPort());

        HotelAPI.setup(app);
    }

    private static int getPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 7070;
    }
}
