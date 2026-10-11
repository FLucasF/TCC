package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends EntregaLinear {

    public Economica() {
        super("12.00", "2.00", 7);
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
