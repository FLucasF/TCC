package br.com.loja.pedidos.checkout;

import java.util.List;
import java.util.Map;

/**
 * The purchase data as received. Nothing is rejected while parsing: values of the wrong shape become null
 * (or 0 for installments) so that {@link CheckoutService} reports problems in the documented order.
 */
public record CheckoutRequest(
        String cpf,
        String email,
        List<Item> items,
        String zipCode,
        String region,
        String shippingMethod,
        boolean couponInformed,
        String coupon,
        String paymentMethod,
        int installments,
        String clubTier) {

    public record Item(String sku, Integer quantity) {
    }

    public static CheckoutRequest parse(Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        Map<?, ?> customer = body.get("customer") instanceof Map<?, ?> m ? m : Map.of();
        List<Item> items = null;
        if (body.get("items") instanceof List<?> list) {
            items = list.stream().map(CheckoutRequest::item).toList();
        }
        return new CheckoutRequest(text(customer.get("cpf")), text(customer.get("email")), items,
                text(body.get("zipCode")), text(body.get("region")), text(body.get("shippingMethod")),
                body.get("coupon") != null, text(body.get("coupon")), text(body.get("paymentMethod")),
                installments(body), text(body.get("clubTier")));
    }

    private static Item item(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) {
            return new Item(null, null);
        }
        Integer quantity = null;
        if (m.get("quantity") instanceof Integer i) {
            quantity = i;
        } else if (m.get("quantity") instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            quantity = l.intValue();
        }
        return new Item(text(m.get("sku")), quantity);
    }

    private static int installments(Map<String, Object> body) {
        Object raw = body.get("installments");
        if (raw == null) {
            return 1;
        }
        if (raw instanceof Integer i) {
            return i;
        }
        if (raw instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        return 0;
    }

    private static String text(Object value) {
        return value instanceof String s ? s : null;
    }
}
