package shop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.util.List;
import shop.app.CheckoutService;
import shop.app.CreditEffect;
import shop.app.Notifier;
import shop.app.OrderService;
import shop.app.PaymentApprovedNoticeEffect;
import shop.app.QueryService;
import shop.app.TransLogClient;
import shop.db.Database;
import shop.db.Schema;
import shop.domain.CarrierGateway;
import shop.domain.Rules;
import shop.web.Api;

public final class Main {
    private Main() {
    }

    /** Wires the whole service on top of a database. */
    public static Api wire(Database db, CarrierGateway carrier, Clock clock) {
        Rules rules = Rules.standard(carrier);
        CheckoutService checkout = new CheckoutService(db, rules);
        Notifier notifier = new Notifier();
        OrderService orders = new OrderService(db, checkout, clock, notifier,
                List.of(new CreditEffect(), new PaymentApprovedNoticeEffect(notifier)));
        return new Api(checkout, orders, new QueryService(db));
    }

    public static void main(String[] args) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(-1);
        HikariDataSource dataSource = new HikariDataSource(config);

        initSchemaWithRetry(dataSource);

        String port = System.getenv("PORT");
        Database db = new Database(dataSource);
        wire(db, new TransLogClient(System.getenv("TRANSLOG_URL")), Clock.systemUTC())
                .create().start(port == null || port.isBlank() ? 8080 : Integer.parseInt(port.trim()));
    }

    private static void initSchemaWithRetry(HikariDataSource dataSource) throws Exception {
        long deadline = System.currentTimeMillis() + 120_000;
        while (true) {
            try {
                Schema.init(dataSource);
                return;
            } catch (Exception e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                System.err.println("Database not ready, retrying: " + e.getMessage());
                Thread.sleep(2000);
            }
        }
    }
}
