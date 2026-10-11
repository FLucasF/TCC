package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CupomMenos50 implements Cupom {
    private static final BigDecimal MINIMO = BigDecimal.valueOf(300.00);
    private static final BigDecimal DESCONTO = BigDecimal.valueOf(50.00);

    @Override public String getCodigo() { return "MENOS50"; }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO.setScale(2);
    }
}
