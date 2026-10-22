package com.pousada.marealta;

import com.pousada.marealta.config.AppConfig;
import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.http.Api;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            AppConfig.load();
            DatabaseConfig.initialize();

            int port = AppConfig.getPort();
            Javalin app = Javalin.create();
            Api.register(app);

            app.start(port);
            logger.info("Servidor rodando na porta {}", port);
        } catch (Exception e) {
            logger.error("Erro ao iniciar aplicação", e);
            System.exit(1);
        }
    }
}
