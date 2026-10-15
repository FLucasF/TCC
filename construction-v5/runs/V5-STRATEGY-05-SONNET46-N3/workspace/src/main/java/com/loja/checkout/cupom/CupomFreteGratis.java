package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return frete;
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
    }
}
