package com.loja.orders.checkout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.common.JsonBodies;
import com.loja.orders.order.OrderItem;

import tools.jackson.databind.JsonNode;

/** Lê os dados da compra e faz a primeira conferência: carrinho e cliente ({@code INVALID_ORDER}). */
@Component
public class CheckoutRequestParser {

    private final JsonBodies bodies;

    public CheckoutRequestParser(JsonBodies bodies) {
        this.bodies = bodies;
    }

    public CheckoutRequest parse(String body) {
        JsonNode root = bodies.read(body);
        if (!root.isObject()) {
            throw invalidOrder();
        }
        List<OrderItem> items = items(root.path("items"));
        JsonNode customer = root.path("customer");
        String cpf = JsonBodies.text(customer, "cpf");
        String email = JsonBodies.text(customer, "email");
        if (cpf == null || email == null) {
            throw invalidOrder();
        }
        JsonNode coupon = root.path("coupon");
        boolean couponInformed = !coupon.isMissingNode() && !coupon.isNull();
        return new CheckoutRequest(
                cpf,
                email,
                items,
                JsonBodies.text(root, "zipCode"),
                JsonBodies.text(root, "region"),
                JsonBodies.text(root, "shippingMethod"),
                couponInformed,
                coupon.isString() ? coupon.stringValue() : null,
                JsonBodies.text(root, "paymentMethod"),
                installments(root.path("installments")),
                JsonBodies.text(root, "clubTier"));
    }

    private static List<OrderItem> items(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            throw invalidOrder();
        }
        List<OrderItem> items = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        for (JsonNode item : node) {
            String sku = JsonBodies.text(item, "sku");
            Integer quantity = integer(item.path("quantity"));
            if (sku == null || !skus.add(sku) || quantity == null || quantity <= 0) {
                throw invalidOrder();
            }
            items.add(new OrderItem(sku, quantity));
        }
        return List.copyOf(items);
    }

    private static Integer installments(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return 1;
        }
        return integer(node);
    }

    private static Integer integer(JsonNode node) {
        if (!node.isNumber() || !node.canConvertToExactIntegral() || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    private static ApiException invalidOrder() {
        return ApiException.unprocessable(ErrorCode.INVALID_ORDER);
    }
}
