package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class EntregaExpressa extends EntregaPorPeso {

    EntregaExpressa() {
        super("EXPRESSA", "25.00", "4.50", 2);
    }
}
