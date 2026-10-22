package br.com.marealta.pousada;

import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        Config config = Config.fromEnv(System.getenv());
        Database db = new Database(require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();

        ReservationService service = new ReservationService(db, new MessageDispatcher(config, Clock.systemUTC()));
        String port = System.getenv("PORT");
        int p = port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
        new WebApp(service).create().start(p);
        if (config.testMode()) System.out.println("MODO DE TESTE: mensagens vão para " + MessageDispatcher.TEST_ADDRESS);
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente ausente: " + name);
        return v;
    }
}
