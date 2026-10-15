package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLojaEntrega implements EntregaStrategy {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }
}
