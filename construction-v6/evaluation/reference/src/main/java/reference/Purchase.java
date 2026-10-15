package reference;

import java.util.List;

/**
 * The purchase data, with the same names as the API's JSON ("Os dados da compra", in the prompt's annex).
 * Any field may be missing (null): the {@link Calculator} decides whether that is an error.
 */
public record Purchase(
        Customer customer,
        List<Item> items,
        String zipCode,
        String region,
        String shippingMethod,
        String coupon,
        String paymentMethod,
        Integer installments,
        String clubTier) {

    public record Customer(String cpf, String email) {}

    public record Item(String sku, Integer quantity) {}
}
