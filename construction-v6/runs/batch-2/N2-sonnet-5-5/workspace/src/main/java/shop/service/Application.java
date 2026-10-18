package shop.service;

import java.time.Clock;
import java.util.List;
import shop.domain.coupon.Coupons;
import shop.domain.payment.PaymentMethods;
import shop.domain.shipping.CarrierClient;
import shop.domain.shipping.ShippingOptions;
import shop.persistence.Store;

/** Montagem dos serviços. */
public record Application(CheckoutService checkout, OrderService orders, QueryService queries) {
    public static Application create(Store store, CarrierClient carrier, Clock clock) {
        CheckoutCalculator calculator = new CheckoutCalculator(ShippingOptions.standard(carrier),
                Coupons.standard(), PaymentMethods.standard());
        // O estoque vem primeiro: é o único que pode recusar o pagamento.
        List<OrderEventListener> listeners = List.of(new StockListener(), new CreditListener(),
                new NotificationListener());
        return new Application(new CheckoutService(store, calculator, clock),
                new OrderService(store, listeners), new QueryService(store));
    }
}
