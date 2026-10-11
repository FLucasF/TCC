package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return Dinheiro.percentual(ctx.subtotalProdutos(), TAXA);
    }
}
