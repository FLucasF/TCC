package br.com.loja.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PedidosApiTest {

    private static HttpServer translog;
    private static volatile String translogBody = "{\"price_cents\":3310,\"eta_hours\":41}";
    private static volatile int translogStatus = 200;
    private static final AtomicInteger cpfs = new AtomicInteger(10_000_000);

    @BeforeAll
    static void startTransLog() throws IOException {
        translog = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        translog.createContext("/v2/quote", exchange -> {
            byte[] body = translogBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(translogStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        translog.start();
    }

    @AfterAll
    static void stopTransLog() {
        translog.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        // o servidor precisa existir antes do contexto
        if (translog == null) {
            startTransLog();
        }
        registry.add("translog.url", () -> "http://127.0.0.1:" + translog.getAddress().getPort());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:pedidos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper mapper;

    private static String newCpf() {
        return String.valueOf(cpfs.incrementAndGet()) + "000";
    }

    private static void carrier(String body) {
        translogStatus = 200;
        translogBody = body;
    }

    private static String purchase(String cpf, String items, String shipping, String coupon, String payment,
                                   int installments, String tier, String region) {
        return """
                {"customer":{"cpf":"%s","email":"a@b.com"},"items":[%s],"zipCode":"01310100","region":"%s",
                 "shippingMethod":"%s",%s"paymentMethod":"%s","installments":%d,"clubTier":"%s"}
                """.formatted(cpf, items, region, shipping, coupon == null ? "" : "\"coupon\":\"" + coupon + "\",",
                payment, installments, tier);
    }

    private ResultActions send(String url, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String register(String body) throws Exception {
        String json = send("/orders", body).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return mapper.readTree(json).get("id").asString();
    }

    private void pay(String id, String tx) throws Exception {
        send("/orders/" + id + "/payment", "{\"transactionId\":\"" + tx + "\",\"paidAt\":\"" + java.time.Instant.now() + "\"}")
                .andExpect(status().isOk());
    }

    private void expectSummary(ResultActions r, String subtotal, String coupon, String shipping, int days,
                               String insurance, String adjustment, String total, int n, String installment,
                               String credit, boolean gift) throws Exception {
        r.andExpect(status().isOk())
                .andExpect(jsonPath("$.productsSubtotal").value(Double.parseDouble(subtotal)))
                .andExpect(jsonPath("$.couponDiscount").value(Double.parseDouble(coupon)))
                .andExpect(jsonPath("$.shippingCost").value(Double.parseDouble(shipping)))
                .andExpect(jsonPath("$.deliveryDays").value(days))
                .andExpect(jsonPath("$.insurance").value(Double.parseDouble(insurance)))
                .andExpect(jsonPath("$.paymentAdjustment").value(Double.parseDouble(adjustment)))
                .andExpect(jsonPath("$.finalTotal").value(Double.parseDouble(total)))
                .andExpect(jsonPath("$.installments").value(n))
                .andExpect(jsonPath("$.installmentAmount").value(Double.parseDouble(installment)))
                .andExpect(jsonPath("$.nextPurchaseCredit").value(Double.parseDouble(credit)))
                .andExpect(jsonPath("$.gift").value(gift));
    }

    private static final String CAM2_TEN1 = "{\"sku\":\"CAM-001\",\"quantity\":2},{\"sku\":\"TEN-001\",\"quantity\":1}";

    @Test
    void example1() throws Exception {
        carrier("{\"price_cents\":3310,\"eta_hours\":41}");
        expectSummary(send("/checkout/summary",
                        purchase(newCpf(), CAM2_TEN1, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH")),
                "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);
    }

    @Test
    void example2() throws Exception {
        carrier("{\"price_cents\":1560,\"eta_hours\":168}");
        expectSummary(send("/checkout/summary",
                        purchase(newCpf(), CAM2_TEN1, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void example3() throws Exception {
        expectSummary(send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"FON-001\",\"quantity\":2}",
                        "COURIER", "MINUS50", "BANK_SLIP", 1, "GOLD", "NORTHEAST")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);
    }

    @Test
    void example4() throws Exception {
        expectSummary(send("/checkout/summary", purchase(newCpf(),
                        "{\"sku\":\"MEI-001\",\"quantity\":7},{\"sku\":\"CAM-001\",\"quantity\":2}",
                        "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void example5() throws Exception {
        carrier("{\"price_cents\":2475,\"eta_hours\":30}");
        expectSummary(send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"CAM-001\",\"quantity\":3}",
                        "EXPRESS", "BUY3PAY2", "PIX", 1, "BRONZE", "SOUTHEAST")),
                "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);
    }

    @Test
    void example6() throws Exception {
        carrier("{\"price_cents\":2210,\"eta_hours\":150}");
        expectSummary(send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"MEI-001\",\"quantity\":1}",
                        "ECONOMY", "FREESHIPPING", "BANK_SLIP", 1, "BRONZE", "SOUTH")),
                "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);
    }

    @Test
    void example7() throws Exception {
        carrier("{\"price_cents\":5080,\"eta_hours\":49}");
        expectSummary(send("/checkout/summary", purchase(newCpf(),
                        "{\"sku\":\"JAQ-001\",\"quantity\":3},{\"sku\":\"TEN-001\",\"quantity\":1}",
                        "EXPRESS", null, "PIX", 1, "GOLD", "NORTH")),
                "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);
    }

    @Test
    void example8() throws Exception {
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"CAM-001\",\"quantity\":1}", "STORE_PICKUP",
                "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("COUPON_NOT_APPLICABLE"));
    }

    @Test
    void example9AndPaidOrderKillsWelcomeCoupon() throws Exception {
        carrier("{\"price_cents\":2900,\"eta_hours\":36}");
        String cpf = newCpf();
        String body = purchase(cpf, "{\"sku\":\"CAM-001\",\"quantity\":2}", "EXPRESS", "WELCOME10", "CREDIT_CARD", 2,
                "BRONZE", "SOUTH");
        String id = register(body);
        send("/checkout/summary", body).andExpect(status().isOk());
        pay(id, "TX-1");
        send("/checkout/summary", body).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("COUPON_NOT_APPLICABLE"));
    }

    @Test
    void example10AndCarrierDown() throws Exception {
        String body = purchase(newCpf(), "{\"sku\":\"TEN-001\",\"quantity\":1}", "ECONOMY", null, "PIX", 1, "BRONZE",
                "NORTH");
        carrier("{\"error\":\"ZIP_NOT_SERVED\"}");
        send("/checkout/summary", body).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("SHIPPING_METHOD_UNAVAILABLE"));
        carrier("{\"price_cents\":1,\"eta_hours\":1}");
        translogStatus = 500;
        send("/checkout/summary", body).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("CARRIER_UNAVAILABLE"));
        translogStatus = 200;
    }

    @Test
    void validationOrder() throws Exception {
        send("/checkout/summary", purchase(newCpf(), "", "PIX", null, "PIX", 1, "GOLD", "NORTH"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("INVALID_ORDER"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"XXX\",\"quantity\":1}", "PIX", null, "PIX", 1, "GOLD",
                "NORTH")).andExpect(jsonPath("$.error").value("UNKNOWN_PRODUCT"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"REL-001\",\"quantity\":1}", "PIX", null, "PIX", 1,
                "GOLD", "NORTH")).andExpect(jsonPath("$.error").value("PRODUCT_DISCONTINUED"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"BON-001\",\"quantity\":1}", "PIX", null, "PIX", 1,
                "GOLD", "NORTH")).andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"CAM-001\",\"quantity\":1}", "COURIER", "", "PIX", 1,
                "GOLD", "NORTH")).andExpect(jsonPath("$.error").value("INVALID_COUPON"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"CAM-001\",\"quantity\":1}", "COURIER", null,
                "BANK_SLIP", 2, "GOLD", "NORTH")).andExpect(jsonPath("$.error").value("INVALID_INSTALLMENTS"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"JAQ-001\",\"quantity\":3}", "STORE_PICKUP", null,
                "BANK_SLIP", 1, "GOLD", "SOUTH")).andExpect(jsonPath("$.error").value("PAYMENT_METHOD_UNAVAILABLE"));
        send("/checkout/summary", purchase(newCpf(), "{\"sku\":\"TEN-001\",\"quantity\":5}", "COURIER", null,
                "PIX", 1, "GOLD", "SOUTH")).andExpect(jsonPath("$.error").value("SHIPPING_METHOD_UNAVAILABLE"));
    }

    @Test
    void paymentLifecycleIsIdempotentAndStockNeverGoesNegative() throws Exception {
        String cpf = newCpf();
        String body = purchase(cpf, "{\"sku\":\"MOC-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1, "SILVER",
                "SOUTH");
        String first = register(body);
        String second = register(body);
        String paidAt = java.time.Instant.now().toString();
        String notice = "{\"transactionId\":\"TX-A\",\"paidAt\":\"" + paidAt + "\"}";

        send("/orders/" + first + "/payment", notice).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        send("/orders/" + first + "/payment", notice).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        send("/orders/" + first + "/payment", "{\"transactionId\":\"TX-B\",\"paidAt\":\"" + paidAt + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("OPERATION_NOT_ALLOWED"));
        mvc.perform(get("/products/MOC-001")).andExpect(jsonPath("$.stock").value(0));
        mvc.perform(get("/customers/" + cpf)).andExpect(jsonPath("$.creditBalance").value(3.2));

        // última peça já foi paga: o segundo pedido é recusado e cancelado, e a recusa se repete
        String lost = "{\"transactionId\":\"TX-C\",\"paidAt\":\"" + paidAt + "\"}";
        send("/orders/" + second + "/payment", lost).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
        send("/orders/" + second + "/payment", lost).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
        mvc.perform(get("/orders/" + second)).andExpect(jsonPath("$.status").value("CANCELLED"));

        // cancelar o pedido pago devolve estoque e tira o crédito
        send("/orders/" + first + "/cancellation", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/products/MOC-001")).andExpect(jsonPath("$.stock").value(1));
        mvc.perform(get("/customers/" + cpf)).andExpect(jsonPath("$.creditBalance").value(0.0));
        mvc.perform(get("/customers/" + cpf + "/notifications"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_APPROVED"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[2].type").value("ORDER_CANCELLED"));
    }

    @Test
    void expirationAndShipping() throws Exception {
        String body = purchase(newCpf(), "{\"sku\":\"MEI-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH");
        String id = register(body);
        String createdAt = mapper.readTree(mvc.perform(get("/orders/" + id)).andReturn().getResponse()
                .getContentAsString()).get("createdAt").asString();
        java.time.Instant created = java.time.Instant.parse(createdAt);

        send("/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}").andExpect(status().isConflict());
        send("/orders/" + id + "/payment", "{\"transactionId\":\"T\",\"paidAt\":\""
                + created.plusSeconds(48 * 3600 + 1) + "\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ORDER_EXPIRED"));
        mvc.perform(get("/orders/" + id)).andExpect(jsonPath("$.status").value("CANCELLED"));

        String ok = register(body);
        created = java.time.Instant.parse(mapper.readTree(mvc.perform(get("/orders/" + ok)).andReturn().getResponse()
                .getContentAsString()).get("createdAt").asString());
        send("/orders/" + ok + "/payment", "{\"transactionId\":\"T\",\"paidAt\":\"" + created.plusSeconds(48 * 3600)
                + "\"}").andExpect(status().isOk());
        send("/orders/" + ok + "/shipment", "{}").andExpect(status().isUnprocessableEntity());
        send("/orders/" + ok + "/shipment", "{\"trackingCode\":\"BR1\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value("BR1"));
        send("/orders/" + ok + "/cancellation", "").andExpect(status().isConflict());
        send("/orders/" + ok + "/delivery", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        send("/orders/nope/delivery", "").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void notificationsCanBeReadAndUnknownResourcesAre404() throws Exception {
        String cpf = newCpf();
        String id = register(purchase(cpf, "{\"sku\":\"MEI-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1,
                "BRONZE", "SOUTH"));
        send("/orders/" + id + "/cancellation", "").andExpect(status().isOk());
        String json = mvc.perform(get("/customers/" + cpf + "/notifications")).andReturn().getResponse()
                .getContentAsString();
        String nid = mapper.readTree(json).get(0).get("id").asString();
        send("/notifications/" + nid + "/read", "").andExpect(jsonPath("$.status").value("READ"));
        send("/notifications/" + nid + "/read", "").andExpect(jsonPath("$.status").value("READ"));
        send("/notifications/zzz/read", "").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_NOT_FOUND"));
        mvc.perform(get("/customers/00000000000/notifications")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
        mvc.perform(get("/products/NOPE")).andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }
}
