package com.loja.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.loja.orders.support.IntegrationTest;

class OrderLifecycleTest extends IntegrationTest {

    private static final String CPF = "12345678909";

    private void expectError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status)).andExpect(content().json("{\"error\":\"" + code + "\"}", true));
    }

    private String now() {
        return Instant.now().toString();
    }

    private ResultActions getJson(String path) throws Exception {
        return mvc.perform(get(path));
    }

    private List<String> notificationTypes(String cpf) throws Exception {
        String body = getJson("/customers/" + cpf + "/notifications").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].type");
    }

    @Test
    void orderSummaryMatchesCheckoutSummary() throws Exception {
        translog.quote(3310, 41);
        String request = checkout().item("CAM-001", 2).item("TEN-001", 1).shipping("EXPRESS").coupon("WELCOME10")
                .region("NORTH").json();
        String summary = summary(request).andReturn().getResponse().getContentAsString();
        String id = placeOrder(request);

        String order = getJson("/orders/" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.trackingCode").value((Object) null))
                .andExpect(jsonPath("$.items[0].sku").value("CAM-001"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].sku").value("TEN-001"))
                .andReturn().getResponse().getContentAsString();
        assertThat(order).contains("\"summary\":" + summary);
        assertThat(order).contains("\"trackingCode\":null");
        assertThat((String) JsonPath.read(order, "$.createdAt")).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ");

        // registrar não baixa estoque; o cliente passa a existir com saldo zero
        assertThat(stock("CAM-001")).isEqualTo(50);
        getJson("/customers/" + CPF).andExpect(status().isOk())
                .andExpect(content().json("{\"cpf\":\"" + CPF + "\",\"creditBalance\":0.00}", true));
        assertThat(notificationTypes(CPF)).isEmpty();
    }

    @Test
    void paymentTakesStockGivesCreditAndNotifies() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 2).item("MEI-001", 3).tier("GOLD").json());
        pay(id, "TX-1", now()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));

        assertThat(stock("CAM-001")).isEqualTo(48);
        assertThat(stock("MEI-001")).isEqualTo(97);
        // 5% de 219,50 = 10,975 -> 10,98
        getJson("/customers/" + CPF).andExpect(content().string("{\"cpf\":\"" + CPF + "\",\"creditBalance\":10.98}"));
        assertThat(notificationTypes(CPF)).containsExactly("PAYMENT_APPROVED");
    }

    @Test
    void repeatedPaymentNoticeIsHarmless() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 2).tier("SILVER").json());
        String first = pay(id, "TX-1", now()).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        String second = pay(id, "TX-1", now()).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(stock("CAM-001")).isEqualTo(48);
        getJson("/customers/" + CPF).andExpect(jsonPath("$.creditBalance").value(3.20));
        assertThat(notificationTypes(CPF)).containsExactly("PAYMENT_APPROVED");

        expectError(pay(id, "TX-2", now()), 409, "OPERATION_NOT_ALLOWED");

        // repetido depois de enviado: o pedido como está
        postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR1\" }").andExpect(status().isOk());
        pay(id, "TX-1", now()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void paymentDeadlineIs48Hours() throws Exception {
        String onTime = placeOrder(checkout().item("CAM-001", 1).json());
        Instant created = Instant.parse(createdAt(onTime));
        pay(onTime, "TX-1", created.plus(Duration.ofHours(48)).toString()).andExpect(status().isOk());

        String late = placeOrder(checkout().item("CAM-001", 1).json());
        Instant lateCreated = Instant.parse(createdAt(late));
        String paidAt = lateCreated.plus(Duration.ofHours(48)).plusSeconds(1).toString();
        expectError(pay(late, "TX-2", paidAt), 409, "ORDER_EXPIRED");
        getJson("/orders/" + late).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock("CAM-001")).isEqualTo(49);

        // o mesmo aviso repetido recebe a mesma recusa; outro aviso, operação não permitida
        expectError(pay(late, "TX-2", paidAt), 409, "ORDER_EXPIRED");
        expectError(pay(late, "TX-3", now()), 409, "OPERATION_NOT_ALLOWED");
        assertThat(notificationTypes(CPF)).containsExactly("PAYMENT_APPROVED", "ORDER_CANCELLED");
    }

    @Test
    void orderWaitsForPaymentEvenAfterDeadline() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 1).json());
        jdbc.sql("UPDATE customer_order SET created_at = created_at - INTERVAL '3' DAY WHERE id = :id")
                .param("id", id).update();
        getJson("/orders/" + id).andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"));
        postJson("/orders/" + id + "/cancellation", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void outOfStockAtPaymentCancelsOrder() throws Exception {
        String first = placeOrder(checkout().item("JAQ-001", 2).json());
        String second = placeOrder(checkout().cpf("98765432100").item("JAQ-001", 2).item("CAM-001", 1).json());
        pay(first, "TX-1", now()).andExpect(status().isOk());

        expectError(pay(second, "TX-2", now()), 409, "INSUFFICIENT_STOCK");
        getJson("/orders/" + second).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock("JAQ-001")).isEqualTo(1);
        assertThat(stock("CAM-001")).isEqualTo(50);
        assertThat(notificationTypes("98765432100")).containsExactly("ORDER_CANCELLED");
        expectError(pay(second, "TX-2", now()), 409, "INSUFFICIENT_STOCK");
    }

    @Test
    void lastPieceIsSoldOnlyOnceUnderConcurrentPayments() throws Exception {
        int buyers = 6;
        List<String> orders = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            orders.add(placeOrder(checkout().cpf("1000000000" + i).item("MOC-001", 1).json()));
        }
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            String id = orders.get(i);
            String tx = "TX-" + i;
            Callable<Integer> payment = () -> {
                start.await();
                return pay(id, tx, now()).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(payment));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(buyers - 1);
        assertThat(stock("MOC-001")).isZero();
    }

    @Test
    void cancellingPaidOrderReturnsStockAndCredit() throws Exception {
        String id = placeOrder(checkout().item("TEN-001", 2).tier("SILVER").json());
        pay(id, "TX-1", now()).andExpect(status().isOk());
        assertThat(stock("TEN-001")).isEqualTo(8);
        getJson("/customers/" + CPF).andExpect(jsonPath("$.creditBalance").value(10.0));

        postJson("/orders/" + id + "/cancellation", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock("TEN-001")).isEqualTo(10);
        getJson("/customers/" + CPF).andExpect(content().string("{\"cpf\":\"" + CPF + "\",\"creditBalance\":0.00}"));
        assertThat(notificationTypes(CPF)).containsExactly("PAYMENT_APPROVED", "ORDER_CANCELLED");
        expectError(postJson("/orders/" + id + "/cancellation", ""), 409, "OPERATION_NOT_ALLOWED");
    }

    @Test
    void cancellingUnpaidOrderOnlyNotifies() throws Exception {
        String id = placeOrder(checkout().item("TEN-001", 2).tier("SILVER").json());
        postJson("/orders/" + id + "/cancellation", "").andExpect(status().isOk());
        assertThat(stock("TEN-001")).isEqualTo(10);
        getJson("/customers/" + CPF).andExpect(jsonPath("$.creditBalance").value(0.0));
        assertThat(notificationTypes(CPF)).containsExactly("ORDER_CANCELLED");
        expectError(pay(id, "TX-1", now()), 409, "OPERATION_NOT_ALLOWED");
    }

    @Test
    void shippingAndDelivery() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 1).json());
        expectError(postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR1\" }"), 409,
                "OPERATION_NOT_ALLOWED");
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
        pay(id, "TX-1", now()).andExpect(status().isOk());

        expectError(postJson("/orders/" + id + "/shipment", "{}"), 422, "INVALID_ORDER");
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
        postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR123456789\" }").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingCode").value("BR123456789"));
        expectError(postJson("/orders/" + id + "/cancellation", ""), 409, "OPERATION_NOT_ALLOWED");
        postJson("/orders/" + id + "/delivery", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
        assertThat(notificationTypes(CPF)).containsExactly("PAYMENT_APPROVED", "ORDER_SHIPPED");
    }

    @Test
    void orderOperationErrors() throws Exception {
        expectError(getJson("/orders/nope"), 404, "ORDER_NOT_FOUND");
        expectError(postJson("/orders/nope/payment", "{}"), 404, "ORDER_NOT_FOUND");
        expectError(postJson("/orders/nope/shipment", "{}"), 404, "ORDER_NOT_FOUND");
        expectError(postJson("/orders/nope/delivery", ""), 404, "ORDER_NOT_FOUND");
        expectError(postJson("/orders/nope/cancellation", ""), 404, "ORDER_NOT_FOUND");

        String id = placeOrder(checkout().item("CAM-001", 1).json());
        expectError(postJson("/orders/" + id + "/payment", "{ \"paidAt\": \"" + now() + "\" }"), 422,
                "INVALID_ORDER");
        expectError(postJson("/orders/" + id + "/payment", "{ \"transactionId\": \"TX\" }"), 422, "INVALID_ORDER");
        expectError(postJson("/orders/" + id + "/payment", "{ \"transactionId\": \"TX\", \"paidAt\": \"ontem\" }"),
                422, "INVALID_ORDER");
        expectError(postJson("/orders/" + id + "/payment", "garbage"), 422, "INVALID_ORDER");
    }

    @Test
    void placeOrderRunsTheSameChecks() throws Exception {
        expectError(postJson("/orders", checkout().item("BON-001", 1).json()), 422, "INSUFFICIENT_STOCK");
        translog.respond(503, "");
        expectError(postJson("/orders", checkout().item("CAM-001", 1).shipping("EXPRESS").json()), 503,
                "CARRIER_UNAVAILABLE");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM customer_order").query(Long.class).single()).isZero();
        expectError(getJson("/customers/" + CPF), 404, "CUSTOMER_NOT_FOUND");
    }

    @Test
    void productsCustomersAndNotifications() throws Exception {
        getJson("/products/CAM-001").andExpect(status().isOk()).andExpect(content().string(
                "{\"sku\":\"CAM-001\",\"name\":\"Camiseta básica\",\"price\":79.90,\"weightKg\":0.30,"
                        + "\"stock\":50,\"discontinued\":false}"));
        getJson("/products/REL-001").andExpect(jsonPath("$.discontinued").value(true));
        expectError(getJson("/products/NOPE"), 404, "PRODUCT_NOT_FOUND");
        expectError(getJson("/customers/00000000000"), 404, "CUSTOMER_NOT_FOUND");
        expectError(getJson("/customers/00000000000/notifications"), 404, "CUSTOMER_NOT_FOUND");
        expectError(postJson("/notifications/nope/read", ""), 404, "NOTIFICATION_NOT_FOUND");

        String id = placeOrder(checkout().item("CAM-001", 1).json());
        pay(id, "TX-1", now()).andExpect(status().isOk());
        String list = getJson("/customers/" + CPF + "/notifications").andReturn().getResponse().getContentAsString();
        String notificationId = JsonPath.read(list, "$[0].id");
        assertThat(list).isEqualTo("[{\"id\":\"" + notificationId + "\",\"type\":\"PAYMENT_APPROVED\",\"orderId\":\""
                + id + "\",\"status\":\"NEW\"}]");

        postJson("/notifications/" + notificationId + "/read", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
        postJson("/notifications/" + notificationId + "/read", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }
}
