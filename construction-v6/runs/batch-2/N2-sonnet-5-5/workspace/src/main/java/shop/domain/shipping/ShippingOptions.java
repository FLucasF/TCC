package shop.domain.shipping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShippingOptions {
    private final Map<String, ShippingOption> byCode = new LinkedHashMap<>();

    public ShippingOptions(List<ShippingOption> options) {
        options.forEach(o -> byCode.put(o.code(), o));
    }

    /** Opções de hoje; uma nova opção de entrega entra nesta lista. */
    public static ShippingOptions standard(CarrierClient carrier) {
        return new ShippingOptions(List.of(
                new CarrierShipping("ECONOMY", "STD", carrier),
                new CarrierShipping("EXPRESS", "EXP", carrier),
                new StorePickup(),
                new Courier()));
    }

    public Optional<ShippingOption> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }
}
