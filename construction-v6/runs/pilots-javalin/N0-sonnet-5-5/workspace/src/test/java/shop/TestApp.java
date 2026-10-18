package shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import shop.db.Database;
import shop.db.Schema;
import shop.domain.CarrierGateway;
import shop.domain.Json;

/** The real service on an in-memory H2 database (PostgreSQL mode) with a scripted carrier. */
final class TestApp implements AutoCloseable {
    record Reply(int status, String raw, JsonNode json) {
    }

    final HikariDataSource dataSource;
    final Javalin server;
    final HttpClient http = HttpClient.newHttpClient();
    volatile Supplier<Optional<CarrierGateway.Quote>> carrier = () -> Optional.of(new CarrierGateway.Quote(
            new java.math.BigDecimal("10.00"), 2));
    volatile Instant now = Instant.parse("2026-10-14T15:30:00Z");

    TestApp() {
        this(null);
    }

    TestApp(HikariDataSource shared) {
        try {
            if (shared == null) {
                HikariConfig cfg = new HikariConfig();
                cfg.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
                cfg.setMaximumPoolSize(8);
                shared = new HikariDataSource(cfg);
                Schema.init(shared);
            }
            dataSource = shared;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        CarrierGateway gateway = (zip, grams, service) -> carrier.get();
        Clock clock = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return now; }
        };
        server = Main.wire(new Database(dataSource), gateway, clock).create().start(0);
    }

    Reply call(String method, String path, String body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path));
            b.header("Content-Type", "application/json");
            b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(r.statusCode(), r.body(), Json.MAPPER.readTree(r.body()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    Reply get(String path) {
        return call("GET", path, null);
    }

    Reply post(String path, String body) {
        return call("POST", path, body);
    }

    void carrierReplies(int cents, int hours) {
        carrier = () -> Optional.of(new CarrierGateway.Quote(java.math.BigDecimal.valueOf(cents, 2), (hours + 23) / 24));
    }

    @Override
    public void close() {
        server.stop();
        dataSource.close();
    }
}
