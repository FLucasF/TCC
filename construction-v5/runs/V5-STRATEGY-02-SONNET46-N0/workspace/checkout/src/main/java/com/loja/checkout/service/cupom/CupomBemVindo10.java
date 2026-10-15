package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CupomBemVindo10 implements Cupom {
    @Override public String getCodigo() { return "BEMVINDO10"; }
    @Override public boolean aplicavel(BigDecimal subtotalProdutos) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
