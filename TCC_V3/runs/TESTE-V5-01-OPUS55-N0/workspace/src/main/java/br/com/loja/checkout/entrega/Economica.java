package br.com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends FretePorPeso {

    public Economica() {
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
