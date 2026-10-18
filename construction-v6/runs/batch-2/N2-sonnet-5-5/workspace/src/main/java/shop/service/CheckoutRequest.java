package shop.service;

import java.util.List;

/** Dados da compra como chegaram; campos podem ser nulos e são validados no cálculo. */
public record CheckoutRequest(String cpf, String email, List<RequestItem> items, String zipCode, String region,
                              String shippingMethod, String coupon, String paymentMethod, int installments,
                              String clubTier) {
    /** quantity é nulo quando ausente ou inválida. */
    public record RequestItem(String sku, Integer quantity) {
    }
}
