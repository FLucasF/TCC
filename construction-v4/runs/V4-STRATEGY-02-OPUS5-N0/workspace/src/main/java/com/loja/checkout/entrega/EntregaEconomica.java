package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, prazo de 7 dias. */
@Component
public class EntregaEconomica extends EntregaBaseMaisPeso {

    public EntregaEconomica() {
        super("12.00", "2.00");
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
