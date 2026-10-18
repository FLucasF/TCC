package shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.domain.Catalog;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Order;
import shop.domain.OrderStatus;
import shop.domain.Summary;
import shop.domain.shipping.CarrierClient;
import shop.persistence.memory.MemoryStore;
import shop.service.Application;
import shop.service.CheckoutRequest;
import shop.service.CheckoutRequest.RequestItem;

class CheckoutExamplesTest {
    static final Instant NOW = Instant.parse("2026-10-14T15:30:00Z");

    MemoryStore store;
    Application app;
    CarrierClient.CarrierQuote carrierQuote;
    boolean zipNotServed;

    @BeforeEach
    void setUp() {
        store = new MemoryStore();
        store.inTransaction(tx -> {
            Catalog.initial().forEach(tx.products()::insertIfAbsent);
            return null;
        });
        CarrierClient carrier = (zip, grams, service) -> {
            if (zipNotServed) {
                throw new CarrierClient.ZipNotServedException();
            }
            return carrierQuote;
        };
        app = Application.create(store, carrier, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    CheckoutRequest req(List<RequestItem> items, String shipping, String coupon, String payment, int inst,
                        String tier, String region) {
        return new CheckoutRequest("12345678909", "ana@exemplo.com", items, "01310100", region, shipping, coupon,
                payment, inst, tier);
    }

    static RequestItem item(String sku, int q) {
        return new RequestItem(sku, q);
    }

    void assertSummary(Summary s, String sub, String disc, String ship, int days, String ins, String adj,
                       String total, int inst, String each, String credit, boolean gift) {
        assertEquals(new BigDecimal(sub), s.productsSubtotal());
        assertEquals(new BigDecimal(disc), s.couponDiscount());
        assertEquals(new BigDecimal(ship), s.shippingCost());
        assertEquals(days, s.deliveryDays());
        assertEquals(new BigDecimal(ins), s.insurance());
        assertEquals(new BigDecimal(adj), s.paymentAdjustment());
        assertEquals(new BigDecimal(total), s.finalTotal());
        assertEquals(inst, s.installments());
        assertEquals(new BigDecimal(each), s.installmentAmount());
        assertEquals(new BigDecimal(credit), s.nextPurchaseCredit());
        assertEquals(gift, s.gift());
    }

    ErrorCode refusal(CheckoutRequest r) {
        return assertThrows(DomainException.class, () -> app.checkout().summary(r)).code();
    }

    @Test
    void example1() {
        carrierQuote = new CarrierClient.CarrierQuote(3310, 41);
        Summary s = app.checkout().summary(req(List.of(item("CAM-001", 2), item("TEN-001", 1)), "EXPRESS",
                "WELCOME10", "PIX", 1, "BRONZE", "NORTH"));
        assertSummary(s, "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);
    }

    @Test
    void example2() {
        carrierQuote = new CarrierClient.CarrierQuote(1560, 168);
        Summary s = app.checkout().summary(req(List.of(item("CAM-001", 2), item("TEN-001", 1)), "ECONOMY",
                null, "CREDIT_CARD", 6, "SILVER", "MIDWEST"));
        assertSummary(s, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void example3() {
        Summary s = app.checkout().summary(req(List.of(item("FON-001", 2)), "COURIER", "MINUS50", "BANK_SLIP", 1,
                "GOLD", "NORTHEAST"));
        assertSummary(s, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);
    }

    @Test
    void example4() {
        Summary s = app.checkout().summary(req(List.of(item("MEI-001", 7), item("CAM-001", 2)), "STORE_PICKUP",
                "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH"));
        assertSummary(s, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void example5() {
        carrierQuote = new CarrierClient.CarrierQuote(2475, 30);
        Summary s = app.checkout().summary(req(List.of(item("CAM-001", 3)), "EXPRESS", "BUY3PAY2", "PIX", 1,
                "BRONZE", "SOUTHEAST"));
        assertSummary(s, "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);
    }

    @Test
    void example6() {
        carrierQuote = new CarrierClient.CarrierQuote(2210, 150);
        Summary s = app.checkout().summary(req(List.of(item("MEI-001", 1)), "ECONOMY", "FREESHIPPING",
                "BANK_SLIP", 1, "BRONZE", "SOUTH"));
        assertSummary(s, "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);
    }

    @Test
    void example7() {
        carrierQuote = new CarrierClient.CarrierQuote(5080, 49);
        Summary s = app.checkout().summary(req(List.of(item("JAQ-001", 3), item("TEN-001", 1)), "EXPRESS", null,
                "PIX", 1, "GOLD", "NORTH"));
        assertSummary(s, "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);
    }

    @Test
    void example8() {
        assertEquals(ErrorCode.COUPON_NOT_APPLICABLE, refusal(req(List.of(item("CAM-001", 1)), "STORE_PICKUP",
                "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST")));
    }

    @Test
    void example9() {
        carrierQuote = new CarrierClient.CarrierQuote(2900, 36);
        payNewOrder(List.of(item("MEI-001", 1)), "tx-0");
        assertEquals(ErrorCode.COUPON_NOT_APPLICABLE, refusal(req(List.of(item("CAM-001", 2)), "EXPRESS",
                "WELCOME10", "CREDIT_CARD", 2, "BRONZE", "SOUTH")));
    }

    @Test
    void example10() {
        zipNotServed = true;
        assertEquals(ErrorCode.SHIPPING_METHOD_UNAVAILABLE, refusal(req(List.of(item("TEN-001", 1)), "ECONOMY",
                null, "PIX", 1, "BRONZE", "NORTH")));
    }

    @Test
    void emptyCouponIsInvalid() {
        assertEquals(ErrorCode.INVALID_COUPON, refusal(req(List.of(item("CAM-001", 1)), "STORE_PICKUP", "",
                "PIX", 1, "BRONZE", "SOUTH")));
    }

    Order payNewOrder(List<RequestItem> items, String tx) {
        Order order = app.checkout().register(req(items, "STORE_PICKUP", null, "PIX", 1, "SILVER", "SOUTH"));
        return app.orders().confirmPayment(order.id(), tx, NOW.plusSeconds(60));
    }

    @Test
    void paymentDebitsStockAndCreditsOnce() {
        Order order = app.checkout().register(req(List.of(item("MOC-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "SILVER", "SOUTH"));
        Order paid = app.orders().confirmPayment(order.id(), "TX-1", NOW.plusSeconds(60));
        Order again = app.orders().confirmPayment(order.id(), "TX-1", NOW.plusSeconds(120));
        assertEquals(OrderStatus.PAID, paid.status());
        assertEquals(OrderStatus.PAID, again.status());
        assertEquals(0, app.queries().product("MOC-001").stock());
        assertEquals(new BigDecimal("3.20"), app.queries().customer("12345678909").creditBalance());
        assertEquals(1, app.queries().notifications("12345678909").size());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(DomainException.class,
                () -> app.orders().confirmPayment(order.id(), "TX-2", NOW)).code());
    }

    @Test
    void lastPieceIsSoldOnlyOnce() {
        Order a = app.checkout().register(req(List.of(item("MOC-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        Order b = app.checkout().register(req(List.of(item("MOC-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        app.orders().confirmPayment(a.id(), "A", NOW);
        DomainException e = assertThrows(DomainException.class, () -> app.orders().confirmPayment(b.id(), "B", NOW));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, e.code());
        assertEquals(409, e.status());
        assertEquals(OrderStatus.CANCELLED, app.orders().get(b.id()).status());
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, assertThrows(DomainException.class,
                () -> app.orders().confirmPayment(b.id(), "B", NOW)).code());
        assertEquals(0, app.queries().product("MOC-001").stock());
    }

    @Test
    void paymentExpiresAfter48Hours() {
        Order exact = app.checkout().register(req(List.of(item("CAM-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        assertEquals(OrderStatus.PAID, app.orders().confirmPayment(exact.id(), "T", NOW.plusSeconds(48 * 3600))
                .status());
        Order late = app.checkout().register(req(List.of(item("CAM-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        assertEquals(ErrorCode.ORDER_EXPIRED, assertThrows(DomainException.class,
                () -> app.orders().confirmPayment(late.id(), "T", NOW.plusSeconds(48 * 3600 + 1))).code());
        assertEquals(OrderStatus.CANCELLED, app.orders().get(late.id()).status());
    }

    @Test
    void cancellingPaidOrderReversesEverything() {
        Order paid = payNewOrder(List.of(item("CAM-001", 2)), "T");
        assertEquals(48, app.queries().product("CAM-001").stock());
        app.orders().cancel(paid.id());
        assertEquals(50, app.queries().product("CAM-001").stock());
        assertEquals(new BigDecimal("0.00"), app.queries().customer("12345678909").creditBalance());
        assertEquals(2, app.queries().notifications("12345678909").size());
        assertTrue(assertThrows(DomainException.class, () -> app.orders().cancel(paid.id())).code()
                == ErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void shipAndDeliverFollowTheFlow() {
        Order order = app.checkout().register(req(List.of(item("CAM-001", 1)), "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(DomainException.class,
                () -> app.orders().ship(order.id(), "BR1")).code());
        app.orders().confirmPayment(order.id(), "T", NOW);
        assertEquals(ErrorCode.INVALID_ORDER, assertThrows(DomainException.class,
                () -> app.orders().ship(order.id(), null)).code());
        assertEquals("BR1", app.orders().ship(order.id(), "BR1").trackingCode());
        assertEquals(OrderStatus.DELIVERED, app.orders().deliver(order.id()).status());
    }
}
