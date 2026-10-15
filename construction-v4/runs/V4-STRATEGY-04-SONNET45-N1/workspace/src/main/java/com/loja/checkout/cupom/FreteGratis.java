package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements CupomStrategy {
    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return frete;
    }
}
