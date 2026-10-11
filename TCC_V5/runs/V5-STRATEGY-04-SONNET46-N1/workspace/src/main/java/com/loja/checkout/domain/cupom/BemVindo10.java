package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

class BemVindo10 implements Cupom {
    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(TAXA);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
