package br.com.marealta;

import org.postgresql.ds.PGSimpleDataSource;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(requireEnv("DB_URL"));
        dataSource.setUser(System.getenv("DB_USER"));
        dataSource.setPassword(System.getenv("DB_PASSWORD"));

        String port = System.getenv("PORT");
        Application.create(dataSource).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
