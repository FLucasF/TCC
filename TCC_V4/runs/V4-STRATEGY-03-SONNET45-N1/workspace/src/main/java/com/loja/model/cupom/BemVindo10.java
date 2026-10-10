package com.loja.model.cupom;

import com.loja.dto.ItemCarrinho;
import com.loja.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
