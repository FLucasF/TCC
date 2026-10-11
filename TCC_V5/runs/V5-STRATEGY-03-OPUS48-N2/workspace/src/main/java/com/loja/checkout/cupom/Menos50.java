package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return contexto.subtotalProdutos().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        return DESCONTO;
    }
}
