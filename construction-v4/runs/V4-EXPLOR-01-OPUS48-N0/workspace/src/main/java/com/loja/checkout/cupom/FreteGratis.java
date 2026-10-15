package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente não paga o frete: o frete aparece normalmente no resumo e o desconto
 * do cupom fica igual ao valor do frete.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        return contexto.frete();
    }
}
