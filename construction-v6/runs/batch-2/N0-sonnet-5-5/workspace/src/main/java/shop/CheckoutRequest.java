package shop;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Dados da compra, lidos do JSON sem rejeitar nada: a conferência (e a ordem dos erros) fica no cálculo. */
record CheckoutRequest(String cpf, String email, List<RawItem> items, String zipCode, String region,
                       String shippingMethod, boolean couponPresent, String coupon, String paymentMethod,
                       int installments, String clubTier) {

    /** sku ou quantity nulos indicam valor ausente ou inválido. */
    record RawItem(String sku, Integer quantity) {}

    static final int INVALID_INSTALLMENTS = -1;

    static CheckoutRequest parse(JsonNode root) {
        JsonNode body = root != null && root.isObject() ? root : Json.MAPPER.createObjectNode();
        JsonNode customer = body.path("customer");
        List<RawItem> items = new ArrayList<>();
        JsonNode itemsNode = body.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode n : itemsNode) {
                String sku = n.isObject() ? text(n.get("sku")) : null;
                JsonNode q = n.isObject() ? n.get("quantity") : null;
                Integer quantity = q != null && q.isIntegralNumber() && q.canConvertToInt() ? q.asInt() : null;
                items.add(new RawItem(sku, quantity));
            }
        }
        JsonNode couponNode = body.get("coupon");
        boolean couponPresent = couponNode != null && !couponNode.isNull();
        JsonNode inst = body.get("installments");
        int installments;
        if (inst == null || inst.isNull()) installments = 1;
        else if (inst.isIntegralNumber() && inst.canConvertToInt()) installments = inst.asInt();
        else installments = INVALID_INSTALLMENTS;
        return new CheckoutRequest(
                customer.isObject() ? text(customer.get("cpf")) : null,
                customer.isObject() ? text(customer.get("email")) : null,
                items, text(body.get("zipCode")), text(body.get("region")), text(body.get("shippingMethod")),
                couponPresent, couponPresent ? couponNode.asText(couponNode.toString()) : null,
                text(body.get("paymentMethod")), installments, text(body.get("clubTier")));
    }

    /** Texto do campo; valores que não são texto viram um texto que nunca é um código válido. */
    private static String text(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return n.isTextual() ? n.asText() : n.toString();
    }
}
