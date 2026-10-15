package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.request.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10"));
    }
}
