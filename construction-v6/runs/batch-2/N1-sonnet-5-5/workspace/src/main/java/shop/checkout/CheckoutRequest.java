package shop.checkout;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The purchase data as it arrived. Fields are kept raw (null when missing or of the wrong type)
 * so that the calculator can report problems in the agreed order.
 */
public record CheckoutRequest(
        String cpf,
        String email,
        List<Item> items,
        String zipCode,
        String region,
        String shippingMethod,
        String coupon,
        String paymentMethod,
        int installments,
        String clubTier) {

    public record Item(String sku, Integer quantity) {
    }

    public static CheckoutRequest from(JsonNode body) {
        JsonNode customer = body.path("customer");
        List<Item> items = null;
        if (body.path("items").isArray()) {
            items = new ArrayList<>();
            for (JsonNode item : body.get("items")) {
                items.add(new Item(text(item, "sku"), quantity(item.get("quantity"))));
            }
        }
        JsonNode coupon = body.get("coupon");
        String couponCode = coupon == null || coupon.isNull() ? null : coupon.isTextual() ? coupon.asText() : coupon.toString();
        JsonNode installments = body.get("installments");
        int count = installments == null || installments.isNull() ? 1
                : installments.isIntegralNumber() && installments.canConvertToInt() ? installments.intValue() : 0;
        return new CheckoutRequest(
                text(customer, "cpf"), text(customer, "email"), items,
                text(body, "zipCode"), text(body, "region"), text(body, "shippingMethod"),
                couponCode, text(body, "paymentMethod"), count, text(body, "clubTier"));
    }

    private static Integer quantity(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
