package br.com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa extends FretePorPeso {

    public Expressa() {
        super(new BigDecimal("25.00"), new BigDecimal("4.50"));
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
