package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class EntregaEconomica extends EntregaPorPeso {

    EntregaEconomica() {
        super("ECONOMICA", "12.00", "2.00", 7);
    }
}
