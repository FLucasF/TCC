package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

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
    public int prazoDias() {
        return 2;
    }
}
