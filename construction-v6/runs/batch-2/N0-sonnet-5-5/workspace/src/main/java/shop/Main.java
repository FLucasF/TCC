package shop;

import io.javalin.Javalin;

import java.time.Clock;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String portText = System.getenv("PORT");
        int port = portText == null || portText.isBlank() ? 8080 : Integer.parseInt(portText.trim());

        JdbcStore store = new JdbcStore(require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        Javalin app = Api.create(store, new TransLogGateway(System.getenv("TRANSLOG_URL")), Clock.systemUTC());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            store.close();
        }));
        app.start(port);
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is not set");
        return value;
    }
}
