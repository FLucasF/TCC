package pousada;

public final class Main {
    public static void main(String[] args) {
        String url = require("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Db db = new Db(url, user, password);
        db.init();
        new App(new Service(db)).create().start(port);
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente ausente: " + name);
        return v;
    }
}
