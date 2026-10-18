package br.com.loja;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.loja.shipping.CarrierGateway;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:loja;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="})
class LojaIntegrationTest {

    static final AtomicReference<Object> CARRIER = new AtomicReference<>();

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        CarrierGateway stubCarrier() {
            return (zip, grams, service) -> {
                Object r = CARRIER.get();
                if (r instanceof CarrierGateway.CarrierQuote q) {
                    return q;
                }
                if (r instanceof CarrierGateway.ZipNotServedException e) {
                    throw e;
                }
                throw new CarrierGateway.CarrierDownException("down", null);
            };
        }
    }

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    record Res(int status, JsonNode body) {
    }

    Res call(String method, String path, String body) throws Exception {
        return send(method, path, body);
    }

    Res send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), json.readTree(r.body()));
    }

    void carrier(int cents, int hours) {
        CARRIER.set(new CarrierGateway.CarrierQuote(BigDecimal.valueOf(cents, 2), (hours + 23) / 24));
    }

    String cart(String cpf, String items, String shipping, String coupon, String payment, int inst, String tier,
                String region) {
        return """
                {"customer":{"cpf":"%s","email":"a@b.com"},"items":[%s],"zipCode":"01310100","region":"%s",
                 "shippingMethod":"%s",%s"paymentMethod":"%s","installments":%d,"clubTier":"%s"}"""
                .formatted(cpf, items, region, shipping, coupon == null ? "" : "\"coupon\":\"" + coupon + "\",",
                        payment, inst, tier);
    }

    void assertSummary(JsonNode s, String subtotal, String coupon, String freight, int days, String insurance,
                       String adjustment, String total, int inst, String instAmount, String credit, boolean gift) {
        assertThat(s.get("productsSubtotal").decimalValue()).isEqualByComparingTo(subtotal);
        assertThat(s.get("couponDiscount").decimalValue()).isEqualByComparingTo(coupon);
        assertThat(s.get("shippingCost").decimalValue()).isEqualByComparingTo(freight);
        assertThat(s.get("deliveryDays").intValue()).isEqualTo(days);
        assertThat(s.get("insurance").decimalValue()).isEqualByComparingTo(insurance);
        assertThat(s.get("paymentAdjustment").decimalValue()).isEqualByComparingTo(adjustment);
        assertThat(s.get("finalTotal").decimalValue()).isEqualByComparingTo(total);
        assertThat(s.get("installments").intValue()).isEqualTo(inst);
        assertThat(s.get("installmentAmount").decimalValue()).isEqualByComparingTo(instAmount);
        assertThat(s.get("nextPurchaseCredit").decimalValue()).isEqualByComparingTo(credit);
        assertThat(s.get("gift").booleanValue()).isEqualTo(gift);
    }

    static final String CAM2_TEN1 = "{\"sku\":\"CAM-001\",\"quantity\":2},{\"sku\":\"TEN-001\",\"quantity\":1}";

    @Test
    void financeExamples() throws Exception {
        carrier(3310, 41);
        Res r = call("POST", "/checkout/summary", cart("10000000001", CAM2_TEN1, "EXPRESS", "WELCOME10", "PIX", 1, "BRONZE", "NORTH"));
        assertThat(r.status()).isEqualTo(200);
        assertSummary(r.body(), "409.70", "40.97", "33.10", 5, "10.24", "0.00", "412.07", 1, "412.07", "0.00", false);

        carrier(1560, 168);
        r = call("POST", "/checkout/summary", cart("10000000002", CAM2_TEN1, "ECONOMY", null, "CREDIT_CARD", 6, "SILVER", "MIDWEST"));
        assertSummary(r.body(), "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);

        r = call("POST", "/checkout/summary", cart("10000000003", "{\"sku\":\"FON-001\",\"quantity\":2}", "COURIER", "MINUS50", "BANK_SLIP", 1, "GOLD", "NORTHEAST"));
        assertSummary(r.body(), "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "19.99", false);

        r = call("POST", "/checkout/summary", cart("10000000004", "{\"sku\":\"MEI-001\",\"quantity\":7},{\"sku\":\"CAM-001\",\"quantity\":2}", "STORE_PICKUP", "BUY3PAY2", "CREDIT_CARD", 3, "SILVER", "SOUTH"));
        assertSummary(r.body(), "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);

        carrier(2475, 30);
        r = call("POST", "/checkout/summary", cart("10000000005", "{\"sku\":\"CAM-001\",\"quantity\":3}", "EXPRESS", "BUY3PAY2", "PIX", 1, "BRONZE", "SOUTHEAST"));
        assertSummary(r.body(), "239.70", "71.91", "24.75", 2, "2.40", "0.00", "194.94", 1, "194.94", "0.00", false);

        carrier(2210, 150);
        r = call("POST", "/checkout/summary", cart("10000000006", "{\"sku\":\"MEI-001\",\"quantity\":1}", "ECONOMY", "FREESHIPPING", "BANK_SLIP", 1, "BRONZE", "SOUTH"));
        assertSummary(r.body(), "19.90", "5.97", "22.10", 7, "0.20", "3.49", "39.72", 1, "39.72", "0.00", false);

        carrier(5080, 49);
        r = call("POST", "/checkout/summary", cart("10000000007", "{\"sku\":\"JAQ-001\",\"quantity\":3},{\"sku\":\"TEN-001\",\"quantity\":1}", "EXPRESS", null, "PIX", 1, "GOLD", "NORTH"));
        assertSummary(r.body(), "1419.60", "0.00", "0.00", 6, "30.00", "-72.48", "1377.12", 1, "1377.12", "70.98", true);

        r = call("POST", "/checkout/summary", cart("10000000008", "{\"sku\":\"CAM-001\",\"quantity\":1}", "STORE_PICKUP", "FREESHIPPING", "PIX", 1, "SILVER", "SOUTHEAST"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.body().get("error").asString()).isEqualTo("COUPON_NOT_APPLICABLE");

        CARRIER.set(new CarrierGateway.ZipNotServedException());
        r = call("POST", "/checkout/summary", cart("10000000010", "{\"sku\":\"TEN-001\",\"quantity\":1}", "ECONOMY", null, "PIX", 1, "BRONZE", "NORTH"));
        assertThat(r.body().get("error").asString()).isEqualTo("SHIPPING_METHOD_UNAVAILABLE");

        CARRIER.set("down");
        r = call("POST", "/checkout/summary", cart("10000000010", "{\"sku\":\"TEN-001\",\"quantity\":1}", "ECONOMY", null, "PIX", 1, "BRONZE", "NORTH"));
        assertThat(r.status()).isEqualTo(503);
        assertThat(r.body().get("error").asString()).isEqualTo("CARRIER_UNAVAILABLE");
    }

    @Test
    void emptyCouponIsInvalid() throws Exception {
        Res r = call("POST", "/checkout/summary", cart("10000000011", "{\"sku\":\"CAM-001\",\"quantity\":1}", "STORE_PICKUP", "", "PIX", 1, "BRONZE", "SOUTH"));
        assertThat(r.body().get("error").asString()).isEqualTo("INVALID_COUPON");
    }

    @Test
    void orderLifecycleAndIdempotentPayment() throws Exception {
        String cpf = "20000000001";
        String body = cart(cpf, "{\"sku\":\"MOC-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1, "GOLD", "SOUTH");
        Res created = call("POST", "/orders", body);
        assertThat(created.status()).isEqualTo(201);
        String id = created.body().get("id").asString();
        assertThat(created.body().get("status").asString()).isEqualTo("AWAITING_PAYMENT");
        assertThat(call("GET", "/customers/" + cpf, null).body().get("creditBalance").decimalValue())
                .isEqualByComparingTo("0");

        // outro cliente tenta comprar a última mochila: ainda há estoque, estoque só baixa no pagamento
        Res second = call("POST", "/orders", cart("20000000002", "{\"sku\":\"MOC-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1, "BRONZE", "SOUTH"));
        String id2 = second.body().get("id").asString();

        String created1 = created.body().get("createdAt").asString();
        String pay = "{\"transactionId\":\"TX-1\",\"paidAt\":\"" + created1 + "\"}";
        Res p = call("POST", "/orders/" + id + "/payment", pay);
        assertThat(p.status()).isEqualTo(200);
        assertThat(p.body().get("status").asString()).isEqualTo("PAID");
        Res again = call("POST", "/orders/" + id + "/payment", pay);
        assertThat(again.body()).isEqualTo(p.body());
        assertThat(call("POST", "/orders/" + id + "/payment", pay.replace("TX-1", "TX-2")).status()).isEqualTo(409);

        assertThat(call("GET", "/products/MOC-001", null).body().get("stock").intValue()).isZero();
        assertThat(call("GET", "/customers/" + cpf, null).body().get("creditBalance").decimalValue())
                .isEqualByComparingTo("8.00");
        assertThat(call("GET", "/customers/" + cpf + "/notifications", null).body()).hasSize(1);

        Res noStock = call("POST", "/orders/" + id2 + "/payment", pay.replace("TX-1", "TX-9"));
        assertThat(noStock.status()).isEqualTo(409);
        assertThat(noStock.body().get("error").asString()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(call("POST", "/orders/" + id2 + "/payment", pay.replace("TX-1", "TX-9")).body())
                .isEqualTo(noStock.body());
        assertThat(call("GET", "/orders/" + id2, null).body().get("status").asString()).isEqualTo("CANCELLED");

        assertThat(call("POST", "/orders/" + id + "/delivery", null).status()).isEqualTo(409);
        assertThat(call("POST", "/orders/" + id + "/shipment", "{}").status()).isEqualTo(422);
        Res shipped = call("POST", "/orders/" + id + "/shipment", "{\"trackingCode\":\"BR1\"}");
        assertThat(shipped.body().get("trackingCode").asString()).isEqualTo("BR1");
        assertThat(call("POST", "/orders/" + id + "/cancellation", null).status()).isEqualTo(409);
        assertThat(call("POST", "/orders/" + id + "/delivery", null).body().get("status").asString())
                .isEqualTo("DELIVERED");
        assertThat(call("GET", "/orders/nope", null).status()).isEqualTo(404);
    }

    @Test
    void paidOrderCancellationRestoresStockAndCredit() throws Exception {
        String cpf = "30000000001";
        Res created = call("POST", "/orders", cart(cpf, "{\"sku\":\"MEI-001\",\"quantity\":10}", "STORE_PICKUP", null, "PIX", 1, "SILVER", "SOUTH"));
        String id = created.body().get("id").asString();
        call("POST", "/orders/" + id + "/payment", "{\"transactionId\":\"T\",\"paidAt\":\"" + created.body().get("createdAt").asString() + "\"}");
        int stockPaid = call("GET", "/products/MEI-001", null).body().get("stock").intValue();
        assertThat(call("POST", "/orders/" + id + "/cancellation", null).status()).isEqualTo(200);
        assertThat(call("GET", "/products/MEI-001", null).body().get("stock").intValue()).isEqualTo(stockPaid + 10);
        assertThat(call("GET", "/customers/" + cpf, null).body().get("creditBalance").decimalValue())
                .isEqualByComparingTo("0");
        assertThat(call("GET", "/customers/" + cpf + "/notifications", null).body()).hasSize(2);
        Res read = call("POST", "/notifications/1/read", null);
        assertThat(read.body().get("status").asString()).isEqualTo("READ");
    }

    @Test
    void paymentExactly48hAcceptedAndAfterExpires() throws Exception {
        Res created = call("POST", "/orders", cart("40000000001", "{\"sku\":\"CAM-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1, "BRONZE", "SOUTH"));
        String id = created.body().get("id").asString();
        java.time.Instant at = java.time.Instant.parse(created.body().get("createdAt").asString());
        Res late = call("POST", "/orders/" + id + "/payment", "{\"transactionId\":\"T\",\"paidAt\":\"" + at.plusSeconds(48 * 3600 + 1) + "\"}");
        assertThat(late.status()).isEqualTo(409);
        assertThat(late.body().get("error").asString()).isEqualTo("ORDER_EXPIRED");

        Res c2 = call("POST", "/orders", cart("40000000001", "{\"sku\":\"CAM-001\",\"quantity\":1}", "STORE_PICKUP", null, "PIX", 1, "BRONZE", "SOUTH"));
        String id2 = c2.body().get("id").asString();
        java.time.Instant at2 = java.time.Instant.parse(c2.body().get("createdAt").asString());
        Res ok = call("POST", "/orders/" + id2 + "/payment", "{\"transactionId\":\"T2\",\"paidAt\":\"" + at2.plusSeconds(48 * 3600) + "\"}");
        assertThat(ok.status()).isEqualTo(200);
    }
}
