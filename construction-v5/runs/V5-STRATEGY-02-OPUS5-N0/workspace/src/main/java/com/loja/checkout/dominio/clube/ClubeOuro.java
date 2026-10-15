package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Ouro: 5% em credito, nunca paga frete e ganha brinde acima de R$ 500,00 em produtos. */
@Component
public class ClubeOuro extends ClubePorPercentual {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    public ClubeOuro() {
        super("0.05");
    }

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
