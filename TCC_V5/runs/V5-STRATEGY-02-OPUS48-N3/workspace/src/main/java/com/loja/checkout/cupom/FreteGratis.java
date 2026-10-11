package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * O cliente não paga o frete: o frete aparece normalmente e o desconto do
 * cupom fica igual ao valor do frete exibido.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        return Dinheiro.centavos(ctx.frete());
    }
}
