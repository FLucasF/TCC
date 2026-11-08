package com.marealta;

public final class Main {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            System.err.println("A variável de ambiente DB_URL é obrigatória");
            System.exit(1);
        }
        Db db = Db.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();
        String port = System.getenv("PORT");
        new App(new ReservationService(db)).create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
