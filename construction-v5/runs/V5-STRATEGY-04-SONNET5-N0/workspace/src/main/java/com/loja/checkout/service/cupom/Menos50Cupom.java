package com.loja.checkout.service.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return DESCONTO;
    }
}
