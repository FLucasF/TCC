package br.com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
public class Expressa extends FretePorPeso {

    public Expressa() {
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
