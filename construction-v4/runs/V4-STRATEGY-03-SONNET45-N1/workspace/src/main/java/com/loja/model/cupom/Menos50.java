package com.loja.model.cupom;

import com.loja.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        return DESCONTO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
