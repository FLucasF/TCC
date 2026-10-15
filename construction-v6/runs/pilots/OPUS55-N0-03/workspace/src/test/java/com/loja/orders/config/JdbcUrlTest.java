package com.loja.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcUrlTest {

    @Test
    void keepsJdbcUrls() {
        assertThat(JdbcUrl.parse("jdbc:postgresql://db:5432/loja"))
                .isEqualTo(new JdbcUrl("jdbc:postgresql://db:5432/loja", null, null));
    }

    @Test
    void convertsProviderUrls() {
        assertThat(JdbcUrl.parse("postgres://loja:s%40nha@db.exemplo.com:5432/loja?sslmode=require"))
                .isEqualTo(new JdbcUrl("jdbc:postgresql://db.exemplo.com:5432/loja?sslmode=require", "loja", "s@nha"));
        assertThat(JdbcUrl.parse("postgresql://db/loja")).isEqualTo(new JdbcUrl("jdbc:postgresql://db/loja", null, null));
    }
}
