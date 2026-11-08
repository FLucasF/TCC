package br.com.mare.pousada;

import br.com.mare.pousada.infra.Db;
import br.com.mare.pousada.web.Api;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Db db = new Db(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        Api.create(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
