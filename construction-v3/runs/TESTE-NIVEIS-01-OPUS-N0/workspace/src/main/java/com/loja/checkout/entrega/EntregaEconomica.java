package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
public class EntregaEconomica extends EntregaPorPeso {

    public EntregaEconomica() {
        super("12.00", "2.00");
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
