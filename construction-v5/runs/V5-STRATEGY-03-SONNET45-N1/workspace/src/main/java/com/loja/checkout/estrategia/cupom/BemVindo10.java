package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return Moeda.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
