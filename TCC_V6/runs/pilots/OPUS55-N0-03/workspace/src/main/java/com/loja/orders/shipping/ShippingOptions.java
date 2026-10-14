package com.loja.orders.shipping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Todas as formas de entrega disponíveis, pelo código. */
@Component
public class ShippingOptions {

    private final Map<String, ShippingOption> byCode;

    public ShippingOptions(List<ShippingOption> options) {
        this.byCode = options.stream().collect(Collectors.toMap(ShippingOption::code, Function.identity()));
    }

    public Optional<ShippingOption> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }
}
