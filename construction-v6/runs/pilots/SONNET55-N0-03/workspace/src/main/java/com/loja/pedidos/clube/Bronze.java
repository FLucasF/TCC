package com.loja.pedidos.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    public String codigo() {
        return "BRONZE";
    }

    public BigDecimal fracaoCredito() {
        return BigDecimal.ZERO;
    }

    public boolean isentaFrete() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
