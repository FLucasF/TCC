package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }
}
