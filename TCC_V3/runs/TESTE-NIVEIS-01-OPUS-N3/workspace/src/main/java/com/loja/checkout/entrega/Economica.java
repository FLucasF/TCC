package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
class Economica extends FretePorPeso {

    Economica() {
        super("12.00", "2.00", 7);
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
