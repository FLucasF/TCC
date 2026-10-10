package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return DESCONTO;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
