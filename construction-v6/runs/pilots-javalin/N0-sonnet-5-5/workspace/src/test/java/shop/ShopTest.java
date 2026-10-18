package shop;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.TestApp.Reply;

class ShopTest {
    TestApp app;

    @BeforeEach
    void start() {
        app = new TestApp();
    }

    @AfterEach
    void stop() {
        app.close();
    }

    static String purchase(String items, String ship, String coupon, String pay, int installments, String tier,
                           String region) {
        return purchase("12345678909", items, ship, coupon, pay, installments, tier, region);
    }

    static String purchase(String cpf, String items, String ship, String coupon, String pay, int installments,
                           String tier, String region) {
        return "{\"customer\":{\"cpf\":\"" + cpf + "\",\"email\":\"a@b.com\"},\"items\":" + items
                + ",\"zipCode\":\"01310100\",\"region\":\"" + region + "\",\"shippingMethod\":\"" + ship + "\""
                + (coupon == null ? "" : ",\"coupon\":\"" + coupon + "\"")
                + ",\"paymentMethod\":\"" + pay + "\",\"installments\":" + installments
                + ",\"clubTier\":\"" + tier + "\"}";
    }

    static String items(Object... skuQty) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < skuQty.length; i += 2) {
            sb.append(i > 0 ? "," : "").append("{\"sku\":\"").append(skuQty[i]).append("\",\"quantity\":")
                    .append(skuQty[i + 1]).append("}");
        }
        return sb.append("]").toString();
    }

    void assertSummary(Reply r, String sub, String cupom, String frete, int prazo, String seguro, String ajuste,
                       String total, int n, String parcela, String credito, boolean brinde) {
        assertEquals(200, r.status(), r.raw());
        assertEquals("{\"productsSubtotal\":" + sub + ",\"couponDiscount\":" + cupom + ",\"shippingCost\":" + frete
                + ",\"deliveryDays\":" + prazo + ",\"insurance\":" + seguro + ",\"paymentAdjustment\":" + ajuste
                + ",\"finalTotal\":" + total + ",\"installments\":" + n + ",\"installmentAmount\":" + parcela
                + ",\"nextPurchaseCredit\":" + credito + ",\"gift\":" + brinde + "}", r.raw());
    }

    Reply summary(String body) {
        return app.post("/checkout/summary", body);
    }

    @Test
    void examples() {
        String cam2ten = items("CAM-001", 2, "TEN-001", 1);
        app.carrierReplies(3310, 41);
        assertSummary(summary(purchase(cam2ten, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH")),
                "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);
        app.carrierReplies(1560, 168);
        assertSummary(summary(purchase(cam2ten, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
        assertSummary(summary(purchase(items("FON-001", 2), "COURIER", "MINUS50", "BANK_SLIP", 1, "GOLD", "NORTHEAST")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);
        assertSummary(summary(purchase(items("MEI-001", 7, "CAM-001", 2), "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 3,
                "SILVER", "SOUTH")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
        app.carrierReplies(2475, 30);
        assertSummary(summary(purchase(items("CAM-001", 3), "EXPRESS", "BUY3PAY2", "PIX", 1, "BRONZE", "SOUTHEAST")),
                "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);
        app.carrierReplies(2210, 150);
        assertSummary(summary(purchase(items("MEI-001", 1), "ECONOMY", "FREESHIPPING", "BANK_SLIP", 1, "BRONZE", "SOUTH")),
                "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);
        app.carrierReplies(5080, 49);
        assertSummary(summary(purchase(items("JAQ-001", 3, "TEN-001", 1), "EXPRESS", null, "PIX", 1, "GOLD", "NORTH")),
                "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);
    }

    void assertError(Reply r, int status, String code) {
        assertEquals(status, r.status(), r.raw());
        assertEquals("{\"error\":\"" + code + "\"}", r.raw());
    }

    @Test
    void refusals() {
        assertError(summary(purchase(items("CAM-001", 1), "STORE_PICKUP", "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST")),
                422, "COUPON_NOT_APPLICABLE");
        app.carrier = Optional::empty;
        assertError(summary(purchase(items("TEN-001", 1), "ECONOMY", null, "PIX", 1, "BRONZE", "NORTH")),
                422, "SHIPPING_METHOD_UNAVAILABLE");
        app.carrier = () -> {
            throw new shop.domain.CarrierGateway.UnavailableException("down", null);
        };
        assertError(summary(purchase(items("TEN-001", 1), "EXPRESS", null, "PIX", 1, "BRONZE", "NORTH")),
                503, "CARRIER_UNAVAILABLE");
        assertError(summary(purchase(items("JAQ-001", 1), "COURIER", "", "PIX", 1, "BRONZE", "NORTH")),
                422, "INVALID_COUPON");
        assertError(summary(purchase(items("JAQ-001", 3), "COURIER", null, "BANK_SLIP", 1, "BRONZE", "SOUTH")),
                422, "PAYMENT_METHOD_UNAVAILABLE");
        assertError(summary(purchase(items("TEN-001", 5, "MOC-001", 2), "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")),
                422, "INSUFFICIENT_STOCK");
        assertError(summary(purchase(items("TEN-001", 5), "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")),
                422, "SHIPPING_METHOD_UNAVAILABLE");
        assertError(summary(purchase(items("REL-001", 1, "XXX", 1), "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")),
                422, "UNKNOWN_PRODUCT");
        assertError(summary(purchase(items("REL-001", 1), "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")),
                422, "PRODUCT_DISCONTINUED");
        assertError(summary(purchase(items("CAM-001", 1, "CAM-001", 1), "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")),
                422, "INVALID_ORDER");
        assertError(summary(purchase(items("CAM-001", 1), "COURIER", null, "PIX", 2, "BRONZE", "SOUTH")),
                422, "INVALID_INSTALLMENTS");
        assertError(summary("garbage"), 422, "INVALID_ORDER");
        assertError(app.get("/orders/nope"), 404, "ORDER_NOT_FOUND");
        assertError(app.get("/products/nope"), 404, "PRODUCT_NOT_FOUND");
        assertError(app.get("/customers/1/notifications"), 404, "CUSTOMER_NOT_FOUND");
        assertError(app.post("/notifications/x/read", null), 404, "NOTIFICATION_NOT_FOUND");
    }

    String register(String cpf, String items, String tier) {
        app.carrierReplies(1000, 24);
        Reply r = app.post("/orders", purchase(cpf, items, "ECONOMY", null, "PIX", 1, tier, "SOUTH"));
        assertEquals(201, r.status(), r.raw());
        assertEquals("AWAITING_PAYMENT", r.json().get("status").asText());
        return r.json().get("id").asText();
    }

    Reply pay(String id, String tx, String paidAt) {
        return app.post("/orders/" + id + "/payment", "{\"transactionId\":\"" + tx + "\",\"paidAt\":\"" + paidAt + "\"}");
    }

    int stock(String sku) {
        return app.get("/products/" + sku).json().get("stock").asInt();
    }

    String credit(String cpf) {
        return app.get("/customers/" + cpf).raw().replaceAll(".*\"creditBalance\":([0-9.]+).*", "$1");
    }

    @Test
    void paymentLifecycleAndIdempotency() {
        String id = register("111", items("CAM-001", 2), "SILVER");
        assertEquals(50, stock("CAM-001"));
        Reply first = pay(id, "TX1", "2026-10-14T16:00:00Z");
        assertEquals(200, first.status(), first.raw());
        assertEquals("PAID", first.json().get("status").asText());
        Reply again = pay(id, "TX1", "2026-10-14T16:00:00Z");
        assertEquals(first.raw(), again.raw());
        assertEquals(48, stock("CAM-001"));
        assertEquals("3.20", credit("111"));
        assertError(pay(id, "TX2", "2026-10-14T16:00:00Z"), 409, "OPERATION_NOT_ALLOWED");
        assertEquals(1, app.get("/customers/111/notifications").json().size());

        assertError(app.post("/orders/" + id + "/shipment", "{}"), 422, "INVALID_ORDER");
        Reply shipped = app.post("/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}");
        assertEquals("SHIPPED", shipped.json().get("status").asText());
        assertEquals("BR1", shipped.json().get("trackingCode").asText());
        assertError(app.post("/orders/" + id + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("DELIVERED", app.post("/orders/" + id + "/delivery", null).json().get("status").asText());
    }

    @Test
    void cancelPaidOrderGivesEverythingBack() {
        String id = register("222", items("CAM-001", 2), "GOLD");
        pay(id, "T", "2026-10-14T16:00:00Z");
        assertEquals(48, stock("CAM-001"));
        assertEquals("7.99", credit("222"));
        assertEquals("CANCELLED", app.post("/orders/" + id + "/cancellation", null).json().get("status").asText());
        assertEquals(50, stock("CAM-001"));
        assertEquals("0.00", credit("222"));
        var notes = app.get("/customers/222/notifications").json();
        assertEquals("PAYMENT_APPROVED", notes.get(0).get("type").asText());
        assertEquals("ORDER_CANCELLED", notes.get(1).get("type").asText());
        String noteId = notes.get(0).get("id").asText();
        assertEquals("READ", app.post("/notifications/" + noteId + "/read", null).json().get("status").asText());
        assertEquals("READ", app.post("/notifications/" + noteId + "/read", null).json().get("status").asText());
    }

    @Test
    void expiryBoundaryAndReplayOfRefusal() {
        String ok = register("333", items("CAM-001", 1), "BRONZE");
        assertEquals(200, pay(ok, "A", "2026-10-16T15:30:00Z").status());
        String late = register("333", items("CAM-001", 1), "BRONZE");
        assertError(pay(late, "B", "2026-10-16T15:30:01Z"), 409, "ORDER_EXPIRED");
        assertError(pay(late, "B", "2026-10-14T15:30:01Z"), 409, "ORDER_EXPIRED");
        assertError(pay(late, "C", "2026-10-14T15:30:01Z"), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CANCELLED", app.get("/orders/" + late).json().get("status").asText());
        assertEquals(1, app.get("/customers/333/notifications").json().findValuesAsText("type").stream()
                .filter("ORDER_CANCELLED"::equals).count());
        assertError(pay(late, "", "x"), 422, "INVALID_ORDER");
    }

    @Test
    void welcomeCouponOnlyForFirstPurchase() {
        app.carrierReplies(2900, 36);
        String body = purchase("444", items("CAM-001", 2), "EXPRESS", "WELCOME10", "CREDIT_CARD", 2, "BRONZE", "SOUTH");
        assertEquals(200, summary(body).status());
        String id = register("444", items("CAM-001", 1), "BRONZE");
        pay(id, "T", "2026-10-14T16:00:00Z");
        assertError(summary(body), 422, "COUPON_NOT_APPLICABLE");
    }

    @Test
    void lastPieceIsSoldOnlyOnce() throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ids.add(register("55" + i, items("MOC-001", 1), "BRONZE"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Reply>> results = new ArrayList<>();
        for (String id : ids) {
            Callable<Reply> task = () -> pay(id, "T-" + id, "2026-10-14T16:00:00Z");
            results.add(pool.submit(task));
        }
        int ok = 0;
        for (Future<Reply> f : results) {
            Reply r = f.get();
            if (r.status() == 200) {
                ok++;
            } else {
                assertError(r, 409, "INSUFFICIENT_STOCK");
            }
        }
        pool.shutdown();
        assertEquals(1, ok);
        assertEquals(0, stock("MOC-001"));
    }

    @Test
    void duplicateNoticesConcurrentlyApplyOnce() throws Exception {
        String id = register("666", items("CAM-001", 1), "SILVER");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<Reply>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(pool.submit(() -> pay(id, "SAME", "2026-10-14T16:00:00Z")));
        }
        for (Future<Reply> f : results) {
            assertEquals(200, f.get().status());
        }
        pool.shutdown();
        assertEquals(49, stock("CAM-001"));
        assertEquals("1.60", credit("666"));
    }

    @Test
    void catalogSurvivesRestartOnSameDatabase() {
        app.post("/orders", "{}");
        try (TestApp second = new TestApp(app.dataSource)) {
            assertEquals(50, second.get("/products/CAM-001").json().get("stock").asInt());
            assertTrue(second.get("/products/REL-001").json().get("discontinued").asBoolean());
        }
    }

    @Test
    void sanityInstant() {
        assertEquals("2026-10-14T15:30:00Z", Instant.parse("2026-10-14T15:30:00Z").toString());
    }
}
