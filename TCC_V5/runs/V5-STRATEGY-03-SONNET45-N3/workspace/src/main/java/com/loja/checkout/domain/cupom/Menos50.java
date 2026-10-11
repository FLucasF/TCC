package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return new BigDecimal("50.00");
    }
}
