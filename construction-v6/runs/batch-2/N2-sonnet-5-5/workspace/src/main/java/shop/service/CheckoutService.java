package shop.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import shop.domain.CartItem;
import shop.domain.Order;
import shop.domain.OrderStatus;
import shop.domain.Summary;
import shop.persistence.Store;

public class CheckoutService {
    private final Store store;
    private final CheckoutCalculator calculator;
    private final Clock clock;

    public CheckoutService(Store store, CheckoutCalculator calculator, Clock clock) {
        this.store = store;
        this.calculator = calculator;
        this.clock = clock;
    }

    public Summary summary(CheckoutRequest request) {
        return store.inTransaction(tx -> calculator.calculate(request, tx));
    }

    public Order register(CheckoutRequest request) {
        return store.inTransaction(tx -> {
            Summary summary = calculator.calculate(request, tx);
            tx.customers().ensure(request.cpf());
            Order order = new Order(UUID.randomUUID().toString(), OrderStatus.AWAITING_PAYMENT,
                    Instant.now(clock).truncatedTo(ChronoUnit.SECONDS), summary,
                    request.items().stream().map(i -> new CartItem(i.sku(), i.quantity())).toList(), null,
                    request.cpf());
            tx.orders().insert(order);
            return order;
        });
    }
}
