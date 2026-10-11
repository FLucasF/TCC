package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Ganha 2% dos produtos de volta em crédito. */
@Component
public class Prata implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, TAXA_CREDITO);
    }

    @Override
    public BigDecimal freteDevido(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
