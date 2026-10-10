package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import com.loja.util.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return Dinheiro.arredondar(desconto);
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }
}
