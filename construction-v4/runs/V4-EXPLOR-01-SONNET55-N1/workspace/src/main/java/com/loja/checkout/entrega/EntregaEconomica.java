package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica extends EntregaPorPeso {
    public EntregaEconomica() {
        super("ECONOMICA", "12.00", "2.00", 7);
    }
}
