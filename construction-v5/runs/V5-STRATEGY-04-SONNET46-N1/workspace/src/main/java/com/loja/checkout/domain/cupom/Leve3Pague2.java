package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
