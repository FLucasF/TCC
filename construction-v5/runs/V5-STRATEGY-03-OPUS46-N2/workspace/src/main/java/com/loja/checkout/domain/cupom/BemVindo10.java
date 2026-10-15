package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }
}
