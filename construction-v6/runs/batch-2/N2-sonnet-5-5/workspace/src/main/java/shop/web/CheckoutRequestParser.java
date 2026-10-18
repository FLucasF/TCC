package shop.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import shop.Json;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.service.CheckoutRequest;

final class CheckoutRequestParser {
    private CheckoutRequestParser() {
    }

    static CheckoutRequest parse(String body) {
        JsonNode root;
        try {
            root = Json.MAPPER.readTree(body == null ? "" : body);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.INVALID_ORDER);
        }
        if (root == null || !root.isObject()) {
            throw new DomainException(ErrorCode.INVALID_ORDER);
        }
        JsonNode customer = root.path("customer");
        List<CheckoutRequest.RequestItem> items = new ArrayList<>();
        if (root.path("items").isArray()) {
            for (JsonNode item : root.get("items")) {
                JsonNode q = item.path("quantity");
                Integer quantity = q.isIntegralNumber() && q.canConvertToInt() ? q.asInt() : null;
                items.add(new CheckoutRequest.RequestItem(text(item.path("sku")), quantity));
            }
        }
        JsonNode coupon = root.path("coupon");
        JsonNode inst = root.path("installments");
        int installments = inst.isMissingNode() || inst.isNull() ? 1
                : inst.isIntegralNumber() && inst.canConvertToInt() ? inst.asInt() : 0;
        return new CheckoutRequest(text(customer.path("cpf")), text(customer.path("email")), items,
                text(root.path("zipCode")), text(root.path("region")), text(root.path("shippingMethod")),
                coupon.isMissingNode() || coupon.isNull() ? null : coupon.asText(),
                text(root.path("paymentMethod")), installments, text(root.path("clubTier")));
    }

    static String text(JsonNode node) {
        return node.isTextual() ? node.asText() : null;
    }
}
