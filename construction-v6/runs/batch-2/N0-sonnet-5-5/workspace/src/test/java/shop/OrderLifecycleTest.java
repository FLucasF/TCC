package shop;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-10-14T15:30:00Z");

    private static String courierOrder(String sku, int qty, String coupon, String tier) {
        return CheckoutExamplesTest.body("[{\"sku\":\"" + sku + "\",\"quantity\":" + qty + "}]", "COURIER", coupon,
                "PIX", 1, tier, "SOUTH");
    }

    private static String pay(String tx, String at) {
        return "{\"transactionId\":\"" + tx + "\",\"paidAt\":\"" + at + "\"}";
    }

    @Test
    void paymentIsIdempotentAndUpdatesStockCreditAndNotifications() {
        try (TestApp app = new TestApp(NOW)) {
            var created = app.post("/orders", courierOrder("FON-001", 2, null, "GOLD"));
            assertEquals(201, created.status());
            String id = created.body().get("id").asText();
            assertEquals("AWAITING_PAYMENT", created.body().get("status").asText());
            assertEquals("2026-10-14T15:30:00Z", created.body().get("createdAt").asText());
            assertEquals(200, app.get("/customers/12345678909").status() == 200 ? 200 : 0);

            var first = app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-16T15:30:00Z"));
            assertEquals(200, first.status());
            assertEquals("PAID", first.body().get("status").asText());
            var again = app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-16T15:30:00Z"));
            assertEquals(200, again.status());
            assertEquals(3, app.get("/products/FON-001").body().get("stock").asInt());
            assertEquals("19.99", app.get("/customers/12345678909").body().get("creditBalance").decimalValue().toPlainString());
            assertEquals(1, app.get("/customers/12345678909/notifications").body().size());

            var other = app.post("/orders/" + id + "/payment", pay("TX-2", "2026-10-16T15:30:00Z"));
            assertEquals(409, other.status());
            assertEquals("OPERATION_NOT_ALLOWED", other.body().get("error").asText());

            assertEquals(409, app.post("/orders/" + id + "/delivery", null).status());
            var shipped = app.post("/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}");
            assertEquals("SHIPPED", shipped.body().get("status").asText());
            assertEquals("BR1", shipped.body().get("trackingCode").asText());
            assertEquals(409, app.post("/orders/" + id + "/cancellation", null).status());
            assertEquals("DELIVERED", app.post("/orders/" + id + "/delivery", null).body().get("status").asText());
        }
    }

    @Test
    void cancellingPaidOrderRestoresStockAndCredit() {
        try (TestApp app = new TestApp(NOW)) {
            String id = app.post("/orders", courierOrder("FON-001", 2, null, "SILVER")).body().get("id").asText();
            app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-14T15:30:00Z"));
            assertEquals(200, app.post("/orders/" + id + "/cancellation", null).status());
            assertEquals(5, app.get("/products/FON-001").body().get("stock").asInt());
            assertEquals("0.00", app.get("/customers/12345678909").body().get("creditBalance").decimalValue().toPlainString());
            var notes = app.get("/customers/12345678909/notifications").body();
            assertEquals("ORDER_CANCELLED", notes.get(1).get("type").asText());
            assertEquals("READ", app.post("/notifications/" + notes.get(0).get("id").asLong() + "/read", null)
                    .body().get("status").asText());
        }
    }

    @Test
    void expiredPaymentCancelsOrderAndReplayGivesSameRefusal() {
        try (TestApp app = new TestApp(NOW)) {
            String id = app.post("/orders", courierOrder("CAM-001", 1, null, "BRONZE")).body().get("id").asText();
            var late = app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-16T15:30:01Z"));
            assertEquals(409, late.status());
            assertEquals("ORDER_EXPIRED", late.body().get("error").asText());
            assertEquals("CANCELLED", app.get("/orders/" + id).body().get("status").asText());
            var replay = app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-14T16:00:00Z"));
            assertEquals("ORDER_EXPIRED", replay.body().get("error").asText());
            assertEquals(50, app.get("/products/CAM-001").body().get("stock").asInt());
        }
    }

    @Test
    void lastPieceIsSoldOnlyOnce() {
        try (TestApp app = new TestApp(NOW)) {
            String a = app.post("/orders", courierOrder("MOC-001", 1, null, "BRONZE")).body().get("id").asText();
            String b = app.post("/orders", courierOrder("MOC-001", 1, null, "BRONZE")).body().get("id").asText();
            assertEquals(200, app.post("/orders/" + a + "/payment", pay("TX-A", "2026-10-14T16:00:00Z")).status());
            var second = app.post("/orders/" + b + "/payment", pay("TX-B", "2026-10-14T16:00:00Z"));
            assertEquals(409, second.status());
            assertEquals("INSUFFICIENT_STOCK", second.body().get("error").asText());
            assertEquals(0, app.get("/products/MOC-001").body().get("stock").asInt());
            assertEquals("CANCELLED", app.get("/orders/" + b).body().get("status").asText());
            assertTrue(app.get("/customers/12345678909/notifications").body().size() >= 2);
        }
    }

    @Test
    void welcomeCouponOnlyForFirstPurchase() {
        try (TestApp app = new TestApp(NOW)) {
            String id = app.post("/orders", courierOrder("CAM-001", 1, "WELCOME10", "BRONZE")).body().get("id").asText();
            assertEquals(201, app.post("/checkout/summary", courierOrder("CAM-001", 1, "WELCOME10", "BRONZE")).status() == 200 ? 201 : 0);
            app.post("/orders/" + id + "/payment", pay("TX-1", "2026-10-14T16:00:00Z"));
            var r = app.post("/checkout/summary", courierOrder("CAM-001", 1, "WELCOME10", "BRONZE"));
            assertEquals("COUPON_NOT_APPLICABLE", r.body().get("error").asText());
        }
    }

    @Test
    void unknownResourcesAndValidation() {
        try (TestApp app = new TestApp(NOW)) {
            assertEquals("ORDER_NOT_FOUND", app.get("/orders/x").body().get("error").asText());
            assertEquals("PRODUCT_NOT_FOUND", app.get("/products/x").body().get("error").asText());
            assertEquals("CUSTOMER_NOT_FOUND", app.get("/customers/1/notifications").body().get("error").asText());
            assertEquals("NOTIFICATION_NOT_FOUND", app.post("/notifications/9/read", null).body().get("error").asText());
            assertEquals(404, app.post("/orders/x/payment", "{}").status());
            String id = app.post("/orders", courierOrder("CAM-001", 1, null, "BRONZE")).body().get("id").asText();
            assertEquals("INVALID_ORDER", app.post("/orders/" + id + "/payment", "{}").body().get("error").asText());
            assertEquals("OPERATION_NOT_ALLOWED", app.post("/orders/" + id + "/shipment", "{\"trackingCode\":\"X\"}")
                    .body().get("error").asText());
        }
    }
}
