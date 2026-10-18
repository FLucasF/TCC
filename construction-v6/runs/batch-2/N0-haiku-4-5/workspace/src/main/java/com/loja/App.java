package com.loja;

import com.loja.api.Routes;
import com.loja.database.Database;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;

import java.lang.reflect.Type;

public class App {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String port = System.getenv("PORT");

        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:postgresql://localhost:5432/orderservice";
        }
        if (dbUser == null || dbUser.isEmpty()) {
            dbUser = "postgres";
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = "postgres";
        }
        if (port == null || port.isEmpty()) {
            port = "8080";
        }

        Database.init(dbUrl, dbUser, dbPassword);

        int portNum = Integer.parseInt(port);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                private final com.google.gson.Gson gson = new com.google.gson.Gson();

                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public Object fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        }).start(portNum);

        Routes.setup(app);

        System.out.println("Order service started on port " + portNum);
    }
}
