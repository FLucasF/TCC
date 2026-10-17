package com.loja.orders.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    protected static final FakeTranslog translog = FakeTranslog.get();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void translogUrl(DynamicPropertyRegistry registry) {
        registry.add("translog.url", translog::url);
    }

    @BeforeEach
    void resetData() {
        translog.reset();
        for (String table : new String[] {"notification", "payment_notice", "order_item", "customer_order",
                "customer", "product"}) {
            jdbc.sql("DELETE FROM " + table).update();
        }
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
    }

    protected ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions summary(String json) throws Exception {
        return postJson("/checkout/summary", json);
    }

    protected String placeOrder(String json) throws Exception {
        String body = postJson("/orders", json)                                .andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected ResultActions pay(String orderId, String transactionId, String paidAt) throws Exception {
        return postJson("/orders/" + orderId + "/payment",
                "{ \"transactionId\": \"" + transactionId + "\", \"paidAt\": \"" + paidAt + "\" }");
    }

    protected String createdAt(String orderId) {
        return jdbc.sql("SELECT created_at FROM customer_order WHERE id = :id").param("id", orderId)
                .query(java.time.OffsetDateTime.class).single().toInstant().toString();
    }

    protected int stock(String sku) {
        return jdbc.sql("SELECT stock FROM product WHERE sku = :sku").param("sku", sku).query(Integer.class).single();
    }

                                                                              
    protected static Checkout checkout() {
        return new Checkout();
    }
}
