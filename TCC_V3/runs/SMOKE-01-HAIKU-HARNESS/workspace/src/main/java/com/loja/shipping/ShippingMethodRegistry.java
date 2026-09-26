package com.loja.shipping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ShippingMethodRegistry {
    private final Map<String, ShippingMethod> methods;

    public ShippingMethodRegistry() {
        methods = new HashMap<>();
        methods.put("ECONOMICA", new EconomicaShipping());
        methods.put("EXPRESSA", new ExpressaShipping());
        methods.put("RETIRADA_LOJA", new RetiradalojaShipping());
        methods.put("MOTOBOY", new MotoboyShipping());
    }

    public ShippingMethod get(String code) {
        return methods.get(code);
    }
}
