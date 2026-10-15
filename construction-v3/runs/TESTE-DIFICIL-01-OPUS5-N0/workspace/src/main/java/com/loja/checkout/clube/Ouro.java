package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * OURO: 5% dos produtos de volta em credito, nunca paga frete,
 * cartao sem juros em ate 6x e brinde acima de R$ 500,00 em produtos.
 */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("5");
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public int parcelasSemJuros() {
        return 6;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
