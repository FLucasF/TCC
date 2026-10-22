package br.com.marealta.pousada;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Sobe o sistema inteiro num schema próprio do PostgreSQL, que some no fim. */
final class TestSystem implements AutoCloseable {
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private final String schema;
    private final Javalin app;
    private final String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

    private TestSystem(String schema, Config config) {
        this.schema = schema;
        this.app = Main.start(config).start(0);
        this.baseUrl = "http://localhost:" + app.port();
    }

    static boolean databaseAvailable() {
        if (URL == null) {
            return false;
        }
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    static TestSystem start(boolean testMode, String messageRules) throws SQLException {
        String schema = "t_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        String separator = URL.contains("?") ? "&" : "?";
        Config config = new Config(0, URL + separator + "currentSchema=" + schema, USER, PASSWORD, testMode, messageRules);
        return new TestSystem(schema, config);
    }

    private static void execute(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    record Response(int status, String contentType, String body) {
    }

    Response get(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
    }

    Response post(String path, String body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private Response send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.headers().firstValue("content-type").orElse(""), response.body());
    }

    @Override
    public void close() throws Exception {
        app.stop();
        execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
    }
}
