package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomBemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
    }
}
