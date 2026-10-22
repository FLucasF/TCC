package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.api.RoomsEndpoints;
import com.pousadamarealta.api.ReservationsEndpoints;
import com.pousadamarealta.api.MessagesEndpoints;
import com.pousadamarealta.api.ReportsEndpoints;

public class Application {
    public static void main(String[] args) {
        Database.initialize();

        Javalin app = Javalin.create()
                .start(Integer.parseInt(System.getenv().getOrDefault("PORT", "7070")));

        RoomsEndpoints.register(app);
        ReservationsEndpoints.register(app);
        MessagesEndpoints.register(app);
        ReportsEndpoints.register(app);
    }
}
