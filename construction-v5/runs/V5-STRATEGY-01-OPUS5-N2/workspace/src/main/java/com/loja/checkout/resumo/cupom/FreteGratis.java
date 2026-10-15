package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** O cliente não paga o frete: o desconto do cupom é o valor do frete. */
public class FreteGratis implements Cupom {

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(base.frete());
    }
}
