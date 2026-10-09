package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg, em 2 dias. */
@Component
public class EntregaExpressa extends EntregaPorPeso {

    public EntregaExpressa() {
        super("25.00", "4.50");
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }
}
