package com.loja.checkout.resumo.clube;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** Só o cadastro: não ganha nada. */
public class Bronze implements NivelClube {

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal frete(BigDecimal freteCalculado) {
        return freteCalculado;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
