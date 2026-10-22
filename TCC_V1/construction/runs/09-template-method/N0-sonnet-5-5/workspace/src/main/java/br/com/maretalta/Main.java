package br.com.maretalta;

import br.com.maretalta.store.PostgresStore;
import br.com.maretalta.web.App;

import java.math.BigDecimal;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String url = required("DB_URL");
        PostgresStore store = new PostgresStore(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        store.initSchema(120_000);

        // A prefeitura muda o valor todo ano: TOURISM_TAX_PER_GUEST_NIGHT sobrescreve o padrão.
        String tax = System.getenv().getOrDefault("TOURISM_TAX_PER_GUEST_NIGHT", "4.50");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        App.create(store, new BigDecimal(tax)).start(port);
    }

    private static String required(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente ausente: " + name);
        return v;
    }
}
