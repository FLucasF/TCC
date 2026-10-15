package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return frete;
    }

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
