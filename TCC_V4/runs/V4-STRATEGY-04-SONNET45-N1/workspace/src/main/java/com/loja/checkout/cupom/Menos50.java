package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements CupomStrategy {
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return subtotalProdutos.compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return DESCONTO;
    }
}
