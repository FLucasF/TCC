package com.loja.pedidos.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = Dinheiro.valor("500.00");

    public String codigo() {
        return "OURO";
    }

    public BigDecimal fracaoCredito() {
        return new BigDecimal("0.05");
    }

    public boolean isentaFrete() {
        return true;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
