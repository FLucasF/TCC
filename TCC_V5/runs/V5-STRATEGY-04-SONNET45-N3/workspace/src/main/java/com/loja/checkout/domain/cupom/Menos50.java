package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ContextoCalculo;
import java.math.BigDecimal;

public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public boolean aplicavel(ContextoCalculo contexto) {
        return contexto.getSubtotalProdutos().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCalculo contexto) {
        return DESCONTO;
    }
}
