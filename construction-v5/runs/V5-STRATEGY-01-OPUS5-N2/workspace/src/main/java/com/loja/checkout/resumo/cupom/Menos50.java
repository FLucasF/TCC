package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** R$ 50,00 de desconto nos produtos, a partir de R$ 300,00 em produtos. */
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(DESCONTO);
    }

    @Override
    public boolean aplicavel(BaseCupom base) {
        return base.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }
}
