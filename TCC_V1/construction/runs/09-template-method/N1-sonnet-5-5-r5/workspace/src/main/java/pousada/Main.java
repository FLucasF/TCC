package pousada;

import pousada.db.Database;
import pousada.db.Schema;
import pousada.web.App;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        Schema.create(db);
        String port = System.getenv("PORT");
        App.create(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
