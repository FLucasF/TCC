package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCupom> itens) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
