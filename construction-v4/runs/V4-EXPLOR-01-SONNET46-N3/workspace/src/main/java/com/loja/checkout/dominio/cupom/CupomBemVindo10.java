package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomBemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
        // sem restrição de valor mínimo
    }
}
