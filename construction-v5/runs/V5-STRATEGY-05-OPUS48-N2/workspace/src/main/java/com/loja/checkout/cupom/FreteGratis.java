package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * No resumo o frete aparece normalmente e o desconto do cupom fica igual ao
 * valor do frete.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        return Dinheiro.centavos(ctx.frete());
    }
}
