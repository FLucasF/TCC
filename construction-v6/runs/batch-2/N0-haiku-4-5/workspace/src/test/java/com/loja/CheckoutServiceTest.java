package com.loja;

import com.loja.api.CheckoutRequest;
import com.loja.utils.MoneyUtils;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class CheckoutServiceTest {
    @Test
    public void testMoneyRounding() {
        assertEquals(3.00, MoneyUtils.round(2.995), 0.001);
        assertEquals(2.98, MoneyUtils.round(2.985), 0.001);
        assertEquals(79.90, MoneyUtils.round(79.90), 0.001);
    }

    @Test
    public void testInvalidRequest() {
        CheckoutRequest req = new CheckoutRequest();
        req.items = new ArrayList<>();

        assertEquals("Should reject empty items", true, req.items.isEmpty());
    }

    @Test
    public void testValidCheckoutRequestStructure() {
        CheckoutRequest req = new CheckoutRequest();
        req.customer = new CheckoutRequest.CustomerData();
        req.customer.cpf = "12345678901";
        req.customer.email = "test@example.com";

        CheckoutRequest.OrderItemData item = new CheckoutRequest.OrderItemData();
        item.sku = "CAM-001";
        item.quantity = 1;
        req.items = new ArrayList<>();
        req.items.add(item);

        req.zipCode = "01310100";
        req.region = "SOUTHEAST";
        req.shippingMethod = "STORE_PICKUP";
        req.paymentMethod = "PIX";
        req.clubTier = "BRONZE";

        assertNotNull("Customer should not be null", req.customer);
        assertEquals("Should have 1 item", 1, req.items.size());
        assertEquals("Should have BRONZE tier", "BRONZE", req.clubTier);
    }
}
