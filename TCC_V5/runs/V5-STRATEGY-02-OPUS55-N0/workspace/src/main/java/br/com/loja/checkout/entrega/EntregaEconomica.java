package br.com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

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
    public int prazoDias() {
        return 7;
    }
}
