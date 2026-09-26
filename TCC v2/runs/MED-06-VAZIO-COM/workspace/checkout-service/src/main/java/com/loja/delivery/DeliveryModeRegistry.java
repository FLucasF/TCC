package com.loja.delivery;

import java.util.HashMap;
import java.util.Map;

public class DeliveryModeRegistry {
    private static final Map<String, DeliveryMode> modes = new HashMap<>();

    static {
        modes.put("ECONOMICA", new EconomicaMode());
        modes.put("EXPRESSA", new ExpressaMode());
        modes.put("RETIRADA_LOJA", new RetiradaLojaMode());
        modes.put("MOTOBOY", new MotoboyCost());
    }

    public static DeliveryMode getMode(String code) {
        return modes.get(code);
    }
}
