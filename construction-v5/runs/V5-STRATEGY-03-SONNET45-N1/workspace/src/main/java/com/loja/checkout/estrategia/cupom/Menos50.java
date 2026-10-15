package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = Moeda.de(50.00);
    private static final BigDecimal MINIMO = Moeda.de(300.00);

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return DESCONTO;
    }

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
