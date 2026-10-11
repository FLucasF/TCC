package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return DESCONTO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
