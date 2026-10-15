package com.loja.orders.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Aceita o endereço do banco tanto no formato JDBC ({@code jdbc:postgresql://host:5432/loja}) quanto
 * no formato comum dos provedores ({@code postgres://usuario:senha@host:5432/loja}).
 */
record JdbcUrl(String url, String user, String password) {

    static JdbcUrl parse(String raw) {
        String value = raw.strip();
        if (value.startsWith("jdbc:")) {
            return new JdbcUrl(value, null, null);
        }
        if (!value.startsWith("postgres://") && !value.startsWith("postgresql://")) {
            return new JdbcUrl("jdbc:" + value, null, null);
        }
        URI uri = URI.create(value);
        String user = null;
        String password = null;
        if (uri.getRawUserInfo() != null) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            user = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : null;
        }
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return new JdbcUrl("jdbc:postgresql://" + uri.getHost() + port + path + query, user, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
