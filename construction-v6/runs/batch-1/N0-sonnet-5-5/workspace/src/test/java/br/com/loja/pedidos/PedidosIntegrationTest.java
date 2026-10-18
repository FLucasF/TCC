package br.com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidosIntegrationTest {

    private static volatile String translogBody = "{\"price_cents\":3310,\"eta_hours\":41}";
    private static volatile int translogStatus = 200;
    private static final HttpServer TRANSLOG = startTranslog();

    private static HttpServer startTranslog() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v2/quote", exchange -> {
                byte[] body = translogBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(translogStatus, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("translog.url", () -> "http://127.0.0.1:" + TRANSLOG.getAddress().getPort());
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void reset() {
        jdbc.sql("DELETE FROM payment_events").update();
        jdbc.sql("DELETE FROM notifications").update();
        jdbc.sql("DELETE FROM order_items").update();
        jdbc.sql("DELETE FROM orders").update();
        jdbc.sql("DELETE FROM customers").update();
        jdbc.sql("UPDATE products SET stock = 50 WHERE sku = 'CAM-001'").update();
        jdbc.sql("UPDATE products SET stock = 1 WHERE sku = 'MOC-001'").update();
        translogStatus = 200;
        translogBody = "{\"price_cents\":3310,\"eta_hours\":41}";
    }

    // ---- helpers

    record Reply(int status, Map<String, Object> json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map(String key) {
            return (Map<String, Object>) json.get(key);
        }
    }

    @SuppressWarnings("unchecked")
    private Reply call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, Object> json = r.body().isBlank() ? Map.of() : mapper.readValue(r.body(), Map.class);
        return new Reply(r.statusCode(), json);
    }

    private static String purchase(String items, String shipping, String coupon, String payment, int installments,
                                   String tier, String region) {
        return """
                {"customer":{"cpf":"12345678909","email":"ana@exemplo.com"},"items":%s,"zipCode":"01310100",
                 "region":"%s","shippingMethod":"%s",%s"paymentMethod":"%s","installments":%d,"clubTier":"%s"}
                """.formatted(items, region, shipping, coupon == null ? "" : "\"coupon\":\"" + coupon + "\",",
                payment, installments, tier);
    }

    private void assertSummary(Reply r, String subtotal, String discount, String shipping, int days, String insurance,
                               String adjustment, String total, int installments, String installment, String credit,
                               boolean gift) {
        assertThat(r.status()).isIn(200, 201);
        Map<String, Object> s = r.json().containsKey("summary") ? r.map("summary") : r.json();
        assertThat(new BigDecimal(s.get("productsSubtotal").toString())).isEqualByComparingTo(subtotal);
        assertThat(new BigDecimal(s.get("couponDiscount").toString())).isEqualByComparingTo(discount);
        assertThat(new BigDecimal(s.get("shippingCost").toString())).isEqualByComparingTo(shipping);
        assertThat(s.get("deliveryDays")).isEqualTo(days);
        assertThat(new BigDecimal(s.get("insurance").toString())).isEqualByComparingTo(insurance);
        assertThat(new BigDecimal(s.get("paymentAdjustment").toString())).isEqualByComparingTo(adjustment);
        assertThat(new BigDecimal(s.get("finalTotal").toString())).isEqualByComparingTo(total);
        assertThat(s.get("installments")).isEqualTo(installments);
        assertThat(new BigDecimal(s.get("installmentAmount").toString())).isEqualByComparingTo(installment);
        assertThat(new BigDecimal(s.get("nextPurchaseCredit").toString())).isEqualByComparingTo(credit);
        assertThat(s.get("gift")).isEqualTo(gift);
    }

    private Reply summary(String body) throws Exception {
        return call("POST", "/checkout/summary", body);
    }

    private static final String CAM2_TEN1 =
            "[{\"sku\":\"CAM-001\",\"quantity\":2},{\"sku\":\"TEN-001\",\"quantity\":1}]";

    // ---- the ten examples from the brief

    @Test
    void example1() throws Exception {
        translogBody = "{\"price_cents\":3310,\"eta_hours\":41}";
        assertSummary(summary(purchase(CAM2_TEN1, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH")),
                "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);
    }

    @Test
    void example2() throws Exception {
        translogBody = "{\"price_cents\":1560,\"eta_hours\":168}";
        assertSummary(summary(purchase(CAM2_TEN1, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void example3() throws Exception {
        assertSummary(summary(purchase("[{\"sku\":\"FON-001\",\"quantity\":2}]", "COURIER", "MINUS50", "BANK_SLIP", 1,
                "GOLD", "NORTHEAST")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);
    }

    @Test
    void example4() throws Exception {
        assertSummary(summary(purchase("[{\"sku\":\"MEI-001\",\"quantity\":7},{\"sku\":\"CAM-001\",\"quantity\":2}]",
                "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void example5() throws Exception {
        translogBody = "{\"price_cents\":2475,\"eta_hours\":30}";
        assertSummary(summary(purchase("[{\"sku\":\"CAM-001\",\"quantity\":3}]", "EXPRESS", "BUY3PAY2", "PIX", 1,
                "BRONZE", "SOUTHEAST")),
                "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);
    }

    @Test
    void example6() throws Exception {
        translogBody = "{\"price_cents\":2210,\"eta_hours\":150}";
        assertSummary(summary(purchase("[{\"sku\":\"MEI-001\",\"quantity\":1}]", "ECONOMY", "FREESHIPPING",
                "BANK_SLIP", 1, "BRONZE", "SOUTH")),
                "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);
    }

    @Test
    void example7() throws Exception {
        jdbc.sql("UPDATE products SET stock = 10 WHERE sku = 'JAQ-001'").update();
        translogBody = "{\"price_cents\":5080,\"eta_hours\":49}";
        assertSummary(summary(purchase("[{\"sku\":\"JAQ-001\",\"quantity\":3},{\"sku\":\"TEN-001\",\"quantity\":1}]",
                "EXPRESS", null, "PIX", 1, "GOLD", "NORTH")),
                "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);
    }

    @Test
    void example8() throws Exception {
        Reply r = summary(purchase("[{\"sku\":\"CAM-001\",\"quantity\":1}]", "STORE_PICKUP", "FREESHIPPING", "PIX", 1,
                "SILVER", "SOUTHEAST"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.json()).containsEntry("error", "COUPON_NOT_APPLICABLE");
    }

    @Test
    void example9() throws Exception {
        String welcome = purchase("[{\"sku\":\"CAM-001\",\"quantity\":2}]", "STORE_PICKUP", null, "PIX", 1, "BRONZE",
                "SOUTH");
        Reply order = call("POST", "/orders", welcome);
        assertThat(order.status()).isEqualTo(201);
        assertThat(call("POST", "/orders/" + order.json().get("id") + "/payment",
                "{\"transactionId\":\"TX-1\",\"paidAt\":\"2026-10-14T16:00:00Z\"}").status()).isEqualTo(200);
        Reply r = summary(purchase("[{\"sku\":\"CAM-001\",\"quantity\":2}]", "STORE_PICKUP", "WELCOME10",
                "CREDIT_CARD", 2, "BRONZE", "SOUTH"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.json()).containsEntry("error", "COUPON_NOT_APPLICABLE");
    }

    @Test
    void example10() throws Exception {
        translogBody = "{\"error\":\"ZIP_NOT_SERVED\"}";
        Reply r = summary(purchase("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "ECONOMY", null, "PIX", 1, "BRONZE",
                "NORTH"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.json()).containsEntry("error", "SHIPPING_METHOD_UNAVAILABLE");
    }

    // ---- errors

    @Test
    void carrierDownIs503() throws Exception {
        translogStatus = 500;
        Reply r = summary(purchase("[{\"sku\":\"TEN-001\",\"quantity\":1}]", "EXPRESS", null, "PIX", 1, "BRONZE",
                "NORTH"));
        assertThat(r.status()).isEqualTo(503);
        assertThat(r.json()).containsEntry("error", "CARRIER_UNAVAILABLE");
    }

    @Test
    void validationOrder() throws Exception {
        String ok = "[{\"sku\":\"CAM-001\",\"quantity\":1}]";
        assertThat(summary("{}").json()).containsEntry("error", "INVALID_ORDER");
        assertThat(summary(purchase("[{\"sku\":\"CAM-001\",\"quantity\":1},{\"sku\":\"CAM-001\",\"quantity\":1}]",
                "COURIER", null, "PIX", 1, "BRONZE", "SOUTH").replace("12345678909", "")).json())
                .containsEntry("error", "INVALID_ORDER");
        assertThat(summary(purchase("[{\"sku\":\"XXX\",\"quantity\":1}]", "COURIER", null, "PIX", 1, "BRONZE", "SOUTH"))
                .json()).containsEntry("error", "UNKNOWN_PRODUCT");
        assertThat(summary(purchase("[{\"sku\":\"REL-001\",\"quantity\":1}]", "COURIER", null, "PIX", 1, "BRONZE",
                "SOUTH")).json()).containsEntry("error", "PRODUCT_DISCONTINUED");
        assertThat(summary(purchase("[{\"sku\":\"BON-001\",\"quantity\":1}]", "COURIER", null, "PIX", 1, "BRONZE",
                "SOUTH")).json()).containsEntry("error", "INSUFFICIENT_STOCK");
        assertThat(summary(purchase(ok, "COURIER", null, "PIX", 1, "PLATINUM", "SOUTH")).json())
                .containsEntry("error", "INVALID_CLUB_TIER");
        assertThat(summary(purchase(ok, "COURIER", null, "PIX", 1, "BRONZE", "MARS")).json())
                .containsEntry("error", "INVALID_REGION");
        assertThat(summary(purchase(ok, "COURIER", null, "PIX", 1, "BRONZE", "SOUTH").replace("01310100", "123"))
                .json()).containsEntry("error", "INVALID_ZIP_CODE");
        assertThat(summary(purchase(ok, "DRONE", null, "PIX", 1, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_SHIPPING_METHOD");
        assertThat(summary(purchase(ok, "COURIER", "", "PIX", 1, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_COUPON");
        assertThat(summary(purchase(ok, "COURIER", "welcome10", "PIX", 1, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_COUPON");
        assertThat(summary(purchase(ok, "COURIER", null, "CASH", 1, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_PAYMENT_METHOD");
        assertThat(summary(purchase(ok, "COURIER", null, "PIX", 2, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_INSTALLMENTS");
        assertThat(summary(purchase(ok, "COURIER", null, "CREDIT_CARD", 13, "BRONZE", "SOUTH")).json())
                .containsEntry("error", "INVALID_INSTALLMENTS");
        assertThat(summary(purchase("[{\"sku\":\"JAQ-001\",\"quantity\":3}]", "STORE_PICKUP", null, "BANK_SLIP", 1,
                "BRONZE", "SOUTH")).json()).containsEntry("error", "PAYMENT_METHOD_UNAVAILABLE");
        assertThat(summary(purchase("[{\"sku\":\"MOC-001\",\"quantity\":1},{\"sku\":\"TEN-001\",\"quantity\":4}]",
                "COURIER", null, "PIX", 1, "BRONZE", "SOUTH")).json()).containsEntry("error",
                "SHIPPING_METHOD_UNAVAILABLE");
    }

    // ---- order life cycle

    private List<Map<String, Object>> notifications() throws Exception {
        HttpResponse<String> raw = http.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/customers/12345678909/notifications")).build(),
                HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(raw.body(), List.class);
    }

    private static final String PAY = "{\"transactionId\":\"TX-123\",\"paidAt\":\"2026-10-14T16:00:00Z\"}";

    private Map<String, Object> register(String sku, int quantity, String tier) throws Exception {
        Reply r = call("POST", "/orders", purchase("[{\"sku\":\"" + sku + "\",\"quantity\":" + quantity + "}]",
                "STORE_PICKUP", null, "PIX", 1, tier, "SOUTH"));
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.json().get("status")).isEqualTo("AWAITING_PAYMENT");
        assertThat(r.json().get("trackingCode")).isNull();
        return r.json();
    }

    private String payAt(Map<String, Object> order, java.time.Duration after) {
        java.time.Instant created = java.time.Instant.parse(order.get("createdAt").toString());
        return "{\"transactionId\":\"TX-9\",\"paidAt\":\"" + created.plus(after) + "\"}";
    }

    @Test
    void fullLifeCycleWithCreditAndNotifications() throws Exception {
        Map<String, Object> order = register("CAM-001", 2, "SILVER");
        String id = order.get("id").toString();
        assertThat(new BigDecimal(call("GET", "/customers/12345678909", null).json().get("creditBalance").toString())).isEqualByComparingTo("0.00");

        assertThat(call("POST", "/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}").json())
                .containsEntry("error", "OPERATION_NOT_ALLOWED");

        Reply paid = call("POST", "/orders/" + id + "/payment", PAY);
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.json().get("status")).isEqualTo("PAID");
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(48);
        // 2% of 159.80
        assertThat(new BigDecimal(call("GET", "/customers/12345678909", null).json().get("creditBalance").toString())).isEqualByComparingTo("3.20");

        // same notice again: same answer, nothing done twice
        Reply again = call("POST", "/orders/" + id + "/payment", PAY);
        assertThat(again.status()).isEqualTo(200);
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(48);
        assertThat(new BigDecimal(call("GET", "/customers/12345678909", null).json().get("creditBalance").toString())).isEqualByComparingTo("3.20");
        // another transaction on an order that is no longer waiting
        assertThat(call("POST", "/orders/" + id + "/payment",
                "{\"transactionId\":\"OTHER\",\"paidAt\":\"2026-10-14T16:00:00Z\"}").status()).isEqualTo(409);

        assertThat(call("POST", "/orders/" + id + "/delivery", null).status()).isEqualTo(409);
        assertThat(call("POST", "/orders/" + id + "/shipment", "{}").json()).containsEntry("error", "INVALID_ORDER");
        Reply shipped = call("POST", "/orders/" + id + "/shipment", "{\"trackingCode\":\"BR123456789\"}");
        assertThat(shipped.json()).containsEntry("status", "SHIPPED").containsEntry("trackingCode", "BR123456789");
        assertThat(call("POST", "/orders/" + id + "/cancellation", null).status()).isEqualTo(409);
        assertThat(call("POST", "/orders/" + id + "/delivery", null).json()).containsEntry("status", "DELIVERED");

        assertThat(notifications()).extracting(n -> (Object) n.get("type")).containsExactly("PAYMENT_APPROVED", "ORDER_SHIPPED");
    }

    @Test
    void notificationsAreListedAndMarkedRead() throws Exception {
        Map<String, Object> order = register("CAM-001", 1, "BRONZE");
        String id = order.get("id").toString();
        call("POST", "/orders/" + id + "/payment", PAY);
        call("POST", "/orders/" + id + "/cancellation", null);

        HttpResponse<String> raw = http.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/customers/12345678909/notifications")).build(),
                HttpResponse.BodyHandlers.ofString());
        List<?> list = mapper.readValue(raw.body(), List.class);
        assertThat(list).hasSize(2);
        Map<?, ?> first = (Map<?, ?>) list.get(0);
        Map<?, ?> second = (Map<?, ?>) list.get(1);
        assertThat(first.get("type")).isEqualTo("PAYMENT_APPROVED");
        assertThat(second.get("type")).isEqualTo("ORDER_CANCELLED");
        assertThat(first.get("status")).isEqualTo("NEW");
        assertThat(first.get("orderId")).isEqualTo(id);

        for (int i = 0; i < 2; i++) {
            assertThat(call("POST", "/notifications/" + first.get("id") + "/read", null).json())
                    .containsEntry("status", "READ");
        }
        assertThat(call("POST", "/notifications/nope/read", null).json())
                .containsEntry("error", "NOTIFICATION_NOT_FOUND");
        assertThat(call("GET", "/customers/000/notifications", null).json())
                .containsEntry("error", "CUSTOMER_NOT_FOUND");
    }

    @Test
    void cancellingPaidOrderGivesEverythingBack() throws Exception {
        Map<String, Object> order = register("CAM-001", 2, "GOLD");
        String id = order.get("id").toString();
        call("POST", "/orders/" + id + "/payment", PAY);
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(48);
        Reply cancelled = call("POST", "/orders/" + id + "/cancellation", null);
        assertThat(cancelled.json()).containsEntry("status", "CANCELLED");
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(50);
        assertThat(new BigDecimal(call("GET", "/customers/12345678909", null).json().get("creditBalance").toString())).isEqualByComparingTo("0.00");
        assertThat(call("POST", "/orders/" + id + "/cancellation", null).status()).isEqualTo(409);
    }

    @Test
    void cancellingUnpaidOrderTouchesNothing() throws Exception {
        Map<String, Object> order = register("CAM-001", 2, "GOLD");
        String id = order.get("id").toString();
        assertThat(call("POST", "/orders/" + id + "/cancellation", null).json()).containsEntry("status", "CANCELLED");
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(50);
        assertThat(new BigDecimal(call("GET", "/customers/12345678909", null).json().get("creditBalance").toString())).isEqualByComparingTo("0.00");
        assertThat(notifications()).extracting(n -> (Object) n.get("type")).containsExactly("ORDER_CANCELLED");
    }

    @Test
    void paymentWindowIs48HoursInclusive() throws Exception {
        Map<String, Object> exact = register("CAM-001", 1, "BRONZE");
        Reply ok = call("POST", "/orders/" + exact.get("id") + "/payment",
                payAt(exact, java.time.Duration.ofHours(48)));
        assertThat(ok.status()).isEqualTo(200);

        Map<String, Object> late = register("CAM-001", 1, "BRONZE");
        String body = payAt(late, java.time.Duration.ofHours(48).plusSeconds(1));
        Reply expired = call("POST", "/orders/" + late.get("id") + "/payment", body);
        assertThat(expired.status()).isEqualTo(409);
        assertThat(expired.json()).containsEntry("error", "ORDER_EXPIRED");
        assertThat(call("GET", "/orders/" + late.get("id"), null).json()).containsEntry("status", "CANCELLED");
        // the repeated notice gets the same refusal
        assertThat(call("POST", "/orders/" + late.get("id") + "/payment", body).json())
                .containsEntry("error", "ORDER_EXPIRED");
        assertThat(call("GET", "/products/CAM-001", null).json().get("stock")).isEqualTo(49);
    }

    @Test
    void lastPieceIsSoldOnlyOnce() throws Exception {
        Map<String, Object> first = register("MOC-001", 1, "BRONZE");
        Map<String, Object> second = register("MOC-001", 1, "BRONZE");
        assertThat(call("POST", "/orders/" + first.get("id") + "/payment", PAY).status()).isEqualTo(200);
        Reply r = call("POST", "/orders/" + second.get("id") + "/payment", PAY);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.json()).containsEntry("error", "INSUFFICIENT_STOCK");
        assertThat(call("GET", "/orders/" + second.get("id"), null).json()).containsEntry("status", "CANCELLED");
        assertThat(call("GET", "/products/MOC-001", null).json().get("stock")).isEqualTo(0);
        assertThat(call("POST", "/orders/" + second.get("id") + "/payment", PAY).json())
                .containsEntry("error", "INSUFFICIENT_STOCK");
    }

    @Test
    void unknownResources() throws Exception {
        assertThat(call("GET", "/orders/nope", null)).satisfies(r -> {
            assertThat(r.status()).isEqualTo(404);
            assertThat(r.json()).containsEntry("error", "ORDER_NOT_FOUND");
        });
        assertThat(call("POST", "/orders/nope/payment", "{}").json()).containsEntry("error", "ORDER_NOT_FOUND");
        assertThat(call("GET", "/products/NOPE", null).json()).containsEntry("error", "PRODUCT_NOT_FOUND");
        assertThat(call("GET", "/customers/1", null).json()).containsEntry("error", "CUSTOMER_NOT_FOUND");
        Map<String, Object> product = call("GET", "/products/CAM-001", null).json();
        assertThat(product).containsEntry("name", "Camiseta básica").containsEntry("discontinued", false);
        assertThat(new BigDecimal(product.get("weightKg").toString())).isEqualByComparingTo("0.30");
    }

    @Test
    void registeredSummaryEqualsPreview() throws Exception {
        String body = purchase(CAM2_TEN1, "EXPRESS", "WELCOME10", "CREDIT_CARD", 5, "GOLD", "NORTHEAST");
        Reply preview = summary(body);
        Reply order = call("POST", "/orders", body);
        assertThat(order.status()).isEqualTo(201);
        assertThat(order.map("summary")).isEqualTo(preview.json());
        assertThat(call("GET", "/orders/" + order.json().get("id"), null).map("summary")).isEqualTo(preview.json());
    }
}
