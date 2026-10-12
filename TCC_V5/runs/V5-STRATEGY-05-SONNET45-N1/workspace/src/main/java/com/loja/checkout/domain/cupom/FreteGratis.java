package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }
}
