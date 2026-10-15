package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
@Component
class Bemvindo10 implements Cupom {

    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(BaseCupom base) {
        return true;
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.percentual(base.subtotalProdutos(), TAXA);
    }
}
