package com.pousadamarealta;

import com.pousadamarealta.config.Configuration;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        int port = Configuration.getPort();
        Javalin app = Configuration.createApplication();
        app.start(port);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Server started on port " + port);
    }
}
