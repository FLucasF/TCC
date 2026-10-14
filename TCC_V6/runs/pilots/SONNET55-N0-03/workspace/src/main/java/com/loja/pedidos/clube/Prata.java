package com.loja.pedidos.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    public String codigo() {
        return "PRATA";
    }

    public BigDecimal fracaoCredito() {
        return new BigDecimal("0.02");
    }

    public boolean isentaFrete() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
