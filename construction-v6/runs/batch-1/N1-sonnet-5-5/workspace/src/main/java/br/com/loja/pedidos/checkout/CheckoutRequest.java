package br.com.loja.pedidos.checkout;

import java.util.List;

import br.com.loja.pedidos.order.OrderItem;

/** Os dados da compra, como o site envia. Valores de código chegam como texto e são conferidos no cálculo. */
public record CheckoutRequest(
        CustomerData customer,
        List<ItemData> items,
        String zipCode,
        String region,
        String shippingMethod,
        String coupon,
        String paymentMethod,
        Integer installments,
        String clubTier) {

    public record CustomerData(String cpf, String email) {
    }

    public record ItemData(String sku, Integer quantity) {
    }

    public List<OrderItem> orderItems() {
        return items.stream().map(item -> new OrderItem(item.sku(), item.quantity())).toList();
    }
}
