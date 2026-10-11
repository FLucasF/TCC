package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Só o cadastro: não ganha nada. */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
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
