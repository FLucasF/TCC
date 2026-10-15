package com.loja.resumo.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
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
