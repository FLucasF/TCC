package com.loja.checkout.estrategia.cupom;

import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements EstrategiaCupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Integer> quantidades) {
        return BigDecimal.ZERO;
    }

    @Override
    public void validar(BigDecimal subtotalProdutos, List<Integer> quantidades) throws IllegalArgumentException {
    }
}
