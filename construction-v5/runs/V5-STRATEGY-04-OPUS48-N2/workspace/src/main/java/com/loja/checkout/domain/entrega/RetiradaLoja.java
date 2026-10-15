package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja extends EntregaLinear {

    public RetiradaLoja() {
        super("0.00", "0.00", 1);
    }

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }
}
