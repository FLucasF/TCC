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
                                                                                                              
    }

    private String now() {
        return Instant.now().toString();
    }

    private ResultActions getJson(String path) throws Exception {
        return mvc.perform(get(path));
    }

    private List<String> notificationTypes(String cpf) throws Exception {
        String body = getJson("/customers/" + cpf + "/notifications")                           .andReturn()
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

        String order = getJson("/orders/" + id)                           
                                                                          
                                                                           
                                                                       
                                                                    
                                                                       
                .andReturn().getResponse().getContentAsString();
                                                             
                                                            
                                                                                                                       

        // registrar não baixa estoque; o cliente passa a existir com saldo zero
        assertThat(stock("CAM-001")).isEqualTo(50);
        getJson("/customers/" + CPF)                           
                                                                                                    ;
                                                     
    }

    @Test
    void paymentTakesStockGivesCreditAndNotifies() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 2).item("MEI-001", 3).tier("GOLD").json());
        pay(id, "TX-1", now())                                                                         ;

                                                   
                                                   
        // 5% de 219,50 = 10,975 -> 10,98
        getJson("/customers/" + CPF)                                                                                 ;
                                                                               
    }

    @Test
    void repeatedPaymentNoticeIsHarmless() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 2).tier("SILVER").json());
        String first = pay(id, "TX-1", now())                           .andReturn().getResponse()
                .getContentAsString();
        String second = pay(id, "TX-1", now())                           .andReturn().getResponse()
                .getContentAsString();

                                            
                                                   
        getJson("/customers/" + CPF)                                                   ;
                                                                               

        expectError(pay(id, "TX-2", now()), 409, "OPERATION_NOT_ALLOWED");

        // repetido depois de enviado: o pedido como está
        postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR1\" }")                           ;
        pay(id, "TX-1", now())                                                                            ;
    }

    @Test
    void paymentDeadlineIs48Hours() throws Exception {
        String onTime = placeOrder(checkout().item("CAM-001", 1).json());
        Instant created = Instant.parse(createdAt(onTime));
        pay(onTime, "TX-1", created.plus(Duration.ofHours(48)).toString())                           ;

        String late = placeOrder(checkout().item("CAM-001", 1).json());
        Instant lateCreated = Instant.parse(createdAt(late));
        String paidAt = lateCreated.plus(Duration.ofHours(48)).plusSeconds(1).toString();
        expectError(pay(late, "TX-2", paidAt), 409, "ORDER_EXPIRED");
        getJson("/orders/" + late)                                                   ;
                                                   

        // o mesmo aviso repetido recebe a mesma recusa; outro aviso, operação não permitida
        expectError(pay(late, "TX-2", paidAt), 409, "ORDER_EXPIRED");
        expectError(pay(late, "TX-3", now()), 409, "OPERATION_NOT_ALLOWED");
                                                                                                  
    }

    @Test
    void orderWaitsForPaymentEvenAfterDeadline() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 1).json());
        jdbc.sql("UPDATE customer_order SET created_at = created_at - INTERVAL '3' DAY WHERE id = :id")
                .param("id", id).update();
        getJson("/orders/" + id)                                                          ;
        postJson("/orders/" + id + "/cancellation", "")                           
                                                                   ;
    }

    @Test
    void outOfStockAtPaymentCancelsOrder() throws Exception {
        String first = placeOrder(checkout().item("JAQ-001", 2).json());
        String second = placeOrder(checkout().cpf("98765432100").item("JAQ-001", 2).item("CAM-001", 1).json());
        pay(first, "TX-1", now())                           ;

        expectError(pay(second, "TX-2", now()), 409, "INSUFFICIENT_STOCK");
        getJson("/orders/" + second)                                                   ;
                                                  
                                                   
                                                                                        
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

                                                                  
                                                                           
                                              
    }

    @Test
    void cancellingPaidOrderReturnsStockAndCredit() throws Exception {
        String id = placeOrder(checkout().item("TEN-001", 2).tier("SILVER").json());
        pay(id, "TX-1", now())                           ;
                                                  
        getJson("/customers/" + CPF)                                                   ;

        postJson("/orders/" + id + "/cancellation", "")                           
                                                                   ;
                                                   
        getJson("/customers/" + CPF)                                                                                ;
                                                                                                  
        expectError(postJson("/orders/" + id + "/cancellation", ""), 409, "OPERATION_NOT_ALLOWED");
    }

    @Test
    void cancellingUnpaidOrderOnlyNotifies() throws Exception {
        String id = placeOrder(checkout().item("TEN-001", 2).tier("SILVER").json());
        postJson("/orders/" + id + "/cancellation", "")                           ;
                                                   
        getJson("/customers/" + CPF)                                                  ;
                                                                              
        expectError(pay(id, "TX-1", now()), 409, "OPERATION_NOT_ALLOWED");
    }

    @Test
    void shippingAndDelivery() throws Exception {
        String id = placeOrder(checkout().item("CAM-001", 1).json());
        expectError(postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR1\" }"), 409,
                "OPERATION_NOT_ALLOWED");
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
        pay(id, "TX-1", now())                           ;

        expectError(postJson("/orders/" + id + "/shipment", "{}"), 422, "INVALID_ORDER");
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
        postJson("/orders/" + id + "/shipment", "{ \"trackingCode\": \"BR123456789\" }")                           
                                                                 
                                                                           ;
        expectError(postJson("/orders/" + id + "/cancellation", ""), 409, "OPERATION_NOT_ALLOWED");
        postJson("/orders/" + id + "/delivery", "")                           
                                                                   ;
        expectError(postJson("/orders/" + id + "/delivery", ""), 409, "OPERATION_NOT_ALLOWED");
                                                                                                
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
                                                                                                        
        expectError(getJson("/customers/" + CPF), 404, "CUSTOMER_NOT_FOUND");
    }

    @Test
    void productsCustomersAndNotifications() throws Exception {
        getJson("/products/CAM-001")                                                       
                                                                                                      
                                                                  ;
        getJson("/products/REL-001")                                                  ;
        expectError(getJson("/products/NOPE"), 404, "PRODUCT_NOT_FOUND");
        expectError(getJson("/customers/00000000000"), 404, "CUSTOMER_NOT_FOUND");
        expectError(getJson("/customers/00000000000/notifications"), 404, "CUSTOMER_NOT_FOUND");
        expectError(postJson("/notifications/nope/read", ""), 404, "NOTIFICATION_NOT_FOUND");

        String id = placeOrder(checkout().item("CAM-001", 1).json());
        pay(id, "TX-1", now())                           ;
        String list = getJson("/customers/" + CPF + "/notifications").andReturn().getResponse().getContentAsString();
        String notificationId = JsonPath.read(list, "$[0].id");
                                                                                                                     
                                                  

        postJson("/notifications/" + notificationId + "/read", "")                           
                                                              ;
        postJson("/notifications/" + notificationId + "/read", "")                           
                                                              ;
    }
}
