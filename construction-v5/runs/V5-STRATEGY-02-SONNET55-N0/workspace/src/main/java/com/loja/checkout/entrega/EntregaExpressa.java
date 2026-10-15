package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa extends EntregaPorPeso {
    public EntregaExpressa() {
        super("EXPRESSA", "25.00", "4.50", 2);
    }
}
