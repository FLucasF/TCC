package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
