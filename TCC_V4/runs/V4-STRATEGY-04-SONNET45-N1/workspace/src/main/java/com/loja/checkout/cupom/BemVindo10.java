package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements CupomStrategy {
    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return Arredondamento.arredondar(desconto);
    }
}
