package com.loja.resumo.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelPrata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public boolean isentoFrete() {
        return false;
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
