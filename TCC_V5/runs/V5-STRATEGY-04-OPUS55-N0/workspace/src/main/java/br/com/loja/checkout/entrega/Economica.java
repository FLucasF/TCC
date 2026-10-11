package br.com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica extends FretePorPeso {

    public Economica() {
        super(new BigDecimal("12.00"), new BigDecimal("2.00"));
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
