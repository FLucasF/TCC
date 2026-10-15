package com.loja.orders.config;

import java.time.Clock;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** O banco vem de {@code DB_URL}, {@code DB_USER} e {@code DB_PASSWORD}. */
    @Bean
    public DataSource dataSource(@Value("${DB_URL}") String url, @Value("${DB_USER:}") String user,
            @Value("${DB_PASSWORD:}") String password) {
        JdbcUrl jdbcUrl = JdbcUrl.parse(url);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl.url());
        dataSource.setUsername(user.isEmpty() ? jdbcUrl.user() : user);
        dataSource.setPassword(password.isEmpty() ? jdbcUrl.password() : password);
        return dataSource;
    }
}
