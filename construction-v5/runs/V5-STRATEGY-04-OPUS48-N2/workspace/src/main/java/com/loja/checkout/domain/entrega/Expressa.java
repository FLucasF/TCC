package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

@Component
public class Expressa extends EntregaLinear {

    public Expressa() {
        super("25.00", "4.50", 2);
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }
}
