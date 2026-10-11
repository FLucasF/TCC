package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Prata implements Nivel {
    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
