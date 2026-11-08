package com.marealta;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

public final class Main {
    public static void main(String[] args) throws InterruptedException {
        String port = System.getenv("PORT");
        DataSource dataSource = dataSource();
        awaitDatabase(dataSource);
        new App(dataSource).create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    private static DataSource dataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(System.getenv("DB_URL"));
        ds.setUser(System.getenv("DB_USER"));
        ds.setPassword(System.getenv("DB_PASSWORD"));
        return ds;
    }

    /** O banco pode demorar a subir junto com o sistema: tenta por até um minuto. */
    private static void awaitDatabase(DataSource dataSource) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try (Connection ignored = dataSource.getConnection()) {
                return;
            } catch (Exception e) {
                if (attempt >= 30) {
                    throw new IllegalStateException("Banco indisponível", e);
                }
                Thread.sleep(2000);
            }
        }
    }
}
