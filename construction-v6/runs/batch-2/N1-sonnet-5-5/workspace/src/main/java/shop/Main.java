package shop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import shop.persistence.PostgresStore;
import shop.persistence.Schema;
import shop.shipping.TransLogGateway;

import java.time.Clock;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        HikariDataSource dataSource = connect();
        Schema.init(dataSource);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        App.create(new PostgresStore(dataSource), new TransLogGateway(System.getenv("TRANSLOG_URL")), Clock.systemUTC())
                .start(port);
    }

    /** The database may still be starting when the service does, so it retries for a while. */
    private static HikariDataSource connect() throws InterruptedException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl(System.getenv("DB_URL")));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        for (int attempt = 1; ; attempt++) {
            try {
                config.setInitializationFailTimeout(1);
                return new HikariDataSource(config);
            } catch (RuntimeException e) {
                if (attempt >= 30) {
                    throw e;
                }
                Thread.sleep(2000);
            }
        }
    }

    private static String jdbcUrl(String url) {
        if (url == null || url.startsWith("jdbc:")) {
            return url;
        }
        return "jdbc:" + url.replaceFirst("^postgres://", "postgresql://");
    }
}
