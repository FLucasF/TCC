package br.com.marealta;

import br.com.marealta.persistence.Database;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Database database = new Database(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        App.create(database).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
