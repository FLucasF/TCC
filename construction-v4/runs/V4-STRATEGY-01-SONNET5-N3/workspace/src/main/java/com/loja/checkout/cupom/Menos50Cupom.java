package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("MENOS50")
public class Menos50Cupom implements Cupom {

    private static final BigDecimal VALOR_DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return VALOR_DESCONTO;
    }
}
