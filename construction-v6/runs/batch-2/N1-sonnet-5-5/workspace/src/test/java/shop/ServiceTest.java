package shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.shipping.CarrierGateway;
import shop.shipping.CarrierQuote;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceTest {
    static final ObjectMapper JSON = new ObjectMapper();

    InMemoryStore store;
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();
    AtomicReference<Supplier<CarrierQuote>> carrier = new AtomicReference<>();
    int port;

    @BeforeEach
    void start() {
        store = new InMemoryStore();
        CarrierGateway gateway = (zip, grams, service) -> carrier.get().get();
        app = App.create(store, gateway, Clock.fixed(Instant.parse("2026-10-14T15:30:00Z"), ZoneOffset.UTC)).start(0);
        port = app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    void carrierReplies(int cents, int hours) {
        carrier.set(() -> new CarrierQuote(java.math.BigDecimal.valueOf(cents, 2), hours));
    }

    record Reply(int status, JsonNode body) {
    }

    Reply call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), JSON.readTree(r.body()));
    }

    static String purchase(String items, String shipping, String coupon, String payment, int installments,
                           String tier, String region) {
        return """
                {"customer":{"cpf":"12345678909","email":"ana@exemplo.com"},"items":%s,"zipCode":"01310100",
                 "region":"%s","shippingMethod":"%s",%s"paymentMethod":"%s","installments":%d,"clubTier":"%s"}"""
                .formatted(items, region, shipping, coupon == null ? "" : "\"coupon\":\"" + coupon + "\",", payment,
                        installments, tier);
    }

    void assertSummary(String request, String expected) throws Exception {
        Reply r = call("POST", "/checkout/summary", request);
        assertEquals(200, r.status(), r.body().toString());
        JsonNode e = JSON.readTree(expected);
        e.fieldNames().forEachRemaining(f -> assertEquals(e.get(f).decimalValue().compareTo(r.body().get(f).decimalValue()), 0,
                f + " in " + r.body()));
        assertEquals(e.get("finalTotal").asText(), r.body().get("finalTotal").asText());
    }

    static final String CAM2_TEN1 = "[{\"sku\":\"CAM-001\",\"quantity\":2},{\"sku\":\"TEN-001\",\"quantity\":1}]";

    @Test
    void example1() throws Exception {
        carrierReplies(3310, 41);
        assertSummary(purchase(CAM2_TEN1, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH"),
                "{\"productsSubtotal\":409.70,\"couponDiscount\":40.97,\"shippingCost\":33.10,\"deliveryDays\":5,"
                        + "\"insurance\":10.24,\"paymentAdjustment\":0.00,\"finalTotal\":412.07,\"installmentAmount\":412.07,"
                        + "\"nextPurchaseCredit\":0.00,\"gift\":false}");
    }

    @Test
    void example2() throws Exception {
        carrierReplies(1560, 168);
        assertSummary(purchase(CAM2_TEN1, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST"),
                "{\"shippingCost\":15.60,\"deliveryDays\":7,\"insurance\":6.15,\"paymentAdjustment\":30.55,"
                        + "\"finalTotal\":462.00,\"installments\":6,\"installmentAmount\":77.00,\"nextPurchaseCredit\":8.19}");
    }

    @Test
    void example3() throws Exception {
        assertSummary(purchase("[{\"sku\":\"FON-001\",\"quantity\":2}]", "COURIER", "MINUS50", "BANK_SLIP", 1, "GOLD", "NORTHEAST"),
                "{\"productsSubtotal\":399.80,\"couponDiscount\":50.00,\"shippingCost\":18.00,\"deliveryDays\":0,"
                        + "\"insurance\":8.00,\"paymentAdjustment\":3.49,\"finalTotal\":379.29,\"nextPurchaseCredit\":19.99}");
    }

    @Test
    void example4() throws Exception {
        assertSummary(purchase("[{\"sku\":\"MEI-001\",\"quantity\":7},{\"sku\":\"CAM-001\",\"quantity\":2}]", "STORE_PICKUP",
                        "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH"),
                "{\"productsSubtotal\":299.10,\"couponDiscount\":39.80,\"deliveryDays\":1,\"insurance\":2.99,"
                        + "\"finalTotal\":262.29,\"installmentAmount\":87.43,\"nextPurchaseCredit\":5.98}");
    }

    @Test
    void example5() throws Exception {
        carrierReplies(2475, 30);
        assertSummary(purchase("[{\"sku\":\"CAM-001\",\"quantity\":3}]", "EXPRESS", "BUY3PAY2", "PIX", 1, "BRONZE", "SOUTHEAST"),
                "{\"couponDiscount\":71.91,\"shippingCost\":24.75,\"deliveryDays\":2,\"insurance\":2.40,\"finalTotal\":194.94}");
    }

    @Test
    void example6() throws Exception {
        carrierReplies(2210, 150);
        assertSummary(purchase("[{\"sku\":\"MEI-001\",\"quantity\":1}]", "ECONOMY", "FREESHIPPING", "BANK_SLIP", 1, "BRONZE", "SOUTH"),
                "{\"couponDiscount\":5.97,\"shippingCost\":22.10,\"deliveryDays\":7,\"insurance\":0.20,"
                        + "\"paymentAdjustment\":3.49,\"finalTotal\":39.72}");
    }

    @Test
    void example7() throws Exception {
        carrierReplies(5080, 49);
        store.products.put("JAQ-001", new shop.catalog.Product("JAQ-001", "J", new java.math.BigDecimal("389.90"),
                new java.math.BigDecimal("0.80"), 10, false));
        assertSummary(purchase("[{\"sku\":\"JAQ-001\",\"quantity\":3},{\"sku\":\"TEN-001\",\"quantity\":1}]", "EXPRESS", null,
                        "PIX", 1, "GOLD", "NORTH"),
                "{\"productsSubtotal\":1419.60,\"shippingCost\":0.00,\"deliveryDays\":6,\"insurance\":30.00,"
                        + "\"paymentAdjustment\":-72.48,\"finalTotal\":1377.12,\"nextPurchaseCredit\":70.98,\"gift\":true}");
    }

    @Test
    void refusals() throws Exception {
        Reply r = call("POST", "/checkout/summary",
                purchase("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "STORE_PICKUP", "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST"));
        assertEquals(422, r.status());
        assertEquals("COUPON_NOT_APPLICABLE", r.body().get("error").asText());

        carrier.set(() -> {
            throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
        });
        r = call("POST", "/checkout/summary",
                purchase("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "ECONOMY", null, "PIX", 1, "BRONZE", "NORTH"));
        assertEquals("SHIPPING_METHOD_UNAVAILABLE", r.body().get("error").asText());

        carrier.set(() -> {
            throw ApiException.unavailable("CARRIER_UNAVAILABLE");
        });
        r = call("POST", "/checkout/summary",
                purchase("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "ECONOMY", null, "PIX", 1, "BRONZE", "NORTH"));
        assertEquals(503, r.status());

        r = call("POST", "/checkout/summary", purchase("[{\"sku\":\"REL-001\",\"quantity\":1}]", "COURIER", null, "PIX", 1, "BRONZE", "NORTH"));
        assertEquals("PRODUCT_DISCONTINUED", r.body().get("error").asText());
        r = call("POST", "/checkout/summary", purchase("[{\"sku\":\"BON-001\",\"quantity\":1}]", "COURIER", "", "PIX", 1, "BRONZE", "NORTH"));
        assertEquals("INSUFFICIENT_STOCK", r.body().get("error").asText());
        r = call("POST", "/checkout/summary", purchase("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "COURIER", "", "PIX", 1, "BRONZE", "NORTH"));
        assertEquals("INVALID_COUPON", r.body().get("error").asText());
        r = call("POST", "/checkout/summary", purchase("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "COURIER", null, "PIX", 2, "BRONZE", "NORTH"));
        assertEquals("INVALID_INSTALLMENTS", r.body().get("error").asText());
        r = call("POST", "/checkout/summary", "{}");
        assertEquals("INVALID_ORDER", r.body().get("error").asText());
    }

    String register(String items, String tier) throws Exception {
        carrierReplies(1000, 24);
        Reply r = call("POST", "/orders", purchase(items, "ECONOMY", null, "PIX", 1, tier, "SOUTHEAST"));
        assertEquals(201, r.status(), r.body().toString());
        assertEquals("AWAITING_PAYMENT", r.body().get("status").asText());
        return r.body().get("id").asText();
    }

    String pay(String id, String tx, String at) {
        return "{\"transactionId\":\"" + tx + "\",\"paidAt\":\"" + at + "\"}";
    }

    @Test
    void paymentIsIdempotentAndMovesStockAndCredit() throws Exception {
        String id = register("[{\"sku\":\"FON-001\",\"quantity\":2}]", "SILVER");
        Reply first = call("POST", "/orders/" + id + "/payment", pay(id, "TX-1", "2026-10-14T16:00:00Z"));
        assertEquals(200, first.status());
        assertEquals("PAID", first.body().get("status").asText());
        Reply again = call("POST", "/orders/" + id + "/payment", pay(id, "TX-1", "2026-10-14T16:00:00Z"));
        assertEquals(first.body(), again.body());
        assertEquals(3, store.products.get("FON-001").stock());
        assertEquals(0, new java.math.BigDecimal("8.00").compareTo(call("GET", "/customers/12345678909", null).body().get("creditBalance").decimalValue()));
        assertEquals(1, call("GET", "/customers/12345678909/notifications", null).body().size());
        assertEquals(409, call("POST", "/orders/" + id + "/payment", pay(id, "TX-2", "2026-10-14T16:00:00Z")).status());

        Reply cancelled = call("POST", "/orders/" + id + "/cancellation", null);
        assertEquals("CANCELLED", cancelled.body().get("status").asText());
        assertEquals(5, store.products.get("FON-001").stock());
        assertEquals(0, call("GET", "/customers/12345678909", null).body().get("creditBalance").decimalValue().signum());
        assertEquals(2, call("GET", "/customers/12345678909/notifications", null).body().size());
    }

    @Test
    void expiryBoundaryAndOutOfStock() throws Exception {
        String id = register("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "BRONZE");
        String late = pay(id, "TX-9", "2026-10-16T15:30:01Z");
        Reply r = call("POST", "/orders/" + id + "/payment", late);
        assertEquals(409, r.status());
        assertEquals("ORDER_EXPIRED", r.body().get("error").asText());
        assertEquals("ORDER_EXPIRED", call("POST", "/orders/" + id + "/payment", late).body().get("error").asText());
        assertEquals("CANCELLED", call("GET", "/orders/" + id, null).body().get("status").asText());

        String exact = register("[{\"sku\":\"MOC-001\",\"quantity\":1}]", "BRONZE");
        String other = register("[{\"sku\":\"MOC-001\",\"quantity\":1}]", "BRONZE");
        assertEquals(200, call("POST", "/orders/" + exact + "/payment", pay(exact, "A", "2026-10-16T15:30:00Z")).status());
        r = call("POST", "/orders/" + other + "/payment", pay(other, "B", "2026-10-14T16:00:00Z"));
        assertEquals("INSUFFICIENT_STOCK", r.body().get("error").asText());
        assertEquals(0, store.products.get("MOC-001").stock());
        assertEquals("CANCELLED", call("GET", "/orders/" + other, null).body().get("status").asText());
    }

    @Test
    void shipmentFlow() throws Exception {
        String id = register("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "BRONZE");
        assertEquals(409, call("POST", "/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}").status());
        call("POST", "/orders/" + id + "/payment", pay(id, "T", "2026-10-14T16:00:00Z"));
        assertEquals("INVALID_ORDER", call("POST", "/orders/" + id + "/shipment", "{}").body().get("error").asText());
        Reply shipped = call("POST", "/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}");
        assertEquals("BR1", shipped.body().get("trackingCode").asText());
        assertEquals(409, call("POST", "/orders/" + id + "/cancellation", null).status());
        assertEquals("DELIVERED", call("POST", "/orders/" + id + "/delivery", null).body().get("status").asText());
        assertEquals(404, call("GET", "/orders/nope", null).status());
        String note = call("GET", "/customers/12345678909/notifications", null).body().get(0).get("id").asText();
        assertEquals("READ", call("POST", "/notifications/" + note + "/read", null).body().get("status").asText());
    }
}
