package com.loja.checkout.entrega;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
public final class EntregaEconomica extends EntregaPorPeso {

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
