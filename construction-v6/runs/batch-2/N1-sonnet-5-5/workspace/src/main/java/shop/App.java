package shop;

import io.javalin.Javalin;
import shop.checkout.CheckoutCalculator;
import shop.coupon.Buy3Pay2Coupon;
import shop.coupon.Coupon;
import shop.coupon.FreeShippingCoupon;
import shop.coupon.Minus50Coupon;
import shop.coupon.Welcome10Coupon;
import shop.notification.Notifier;
import shop.order.CreditReaction;
import shop.order.OrderService;
import shop.order.PaymentApprovedNoticeReaction;
import shop.payment.BankSlipPayment;
import shop.payment.CreditCardPayment;
import shop.payment.PaymentOption;
import shop.payment.PixPayment;
import shop.persistence.Store;
import shop.shipping.CarrierGateway;
import shop.shipping.CarrierShipping;
import shop.shipping.CourierShipping;
import shop.shipping.ShippingOption;
import shop.shipping.StorePickupShipping;
import shop.web.Routes;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Wires the service together. Adding a shipping option, coupon, payment method or payment reaction is a line here. */
public final class App {
    private App() {
    }

    public static Javalin create(Store store, CarrierGateway translog, Clock clock) {
        Map<String, ShippingOption> shipping = byCode(List.of(
                new CarrierShipping("ECONOMY", "STD", translog),
                new CarrierShipping("EXPRESS", "EXP", translog),
                new StorePickupShipping(),
                new CourierShipping()), ShippingOption::code);
        Map<String, Coupon> coupons = byCode(List.of(
                new Welcome10Coupon(), new Minus50Coupon(), new FreeShippingCoupon(), new Buy3Pay2Coupon()), Coupon::code);
        Map<String, PaymentOption> payments = byCode(List.of(
                new PixPayment(), new CreditCardPayment(), new BankSlipPayment()), PaymentOption::code);

        Notifier notifier = new Notifier();
        OrderService orders = new OrderService(store, new CheckoutCalculator(shipping, coupons, payments), notifier,
                List.of(new CreditReaction(), new PaymentApprovedNoticeReaction(notifier)), clock);

        Javalin app = Javalin.create();
        new Routes(orders, new QueryService(store)).register(app);
        return app;
    }

    private static <T> Map<String, T> byCode(List<T> items, Function<T, String> code) {
        return items.stream().collect(Collectors.toUnmodifiableMap(code, Function.identity()));
    }
}
