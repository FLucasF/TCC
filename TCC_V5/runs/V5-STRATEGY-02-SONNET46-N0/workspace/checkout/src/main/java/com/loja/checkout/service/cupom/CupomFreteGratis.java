package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CupomFreteGratis implements Cupom {
    @Override public String getCodigo() { return "FRETEGRATIS"; }
    @Override public boolean aplicavel(BigDecimal subtotalProdutos) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete.setScale(2);
    }
}
