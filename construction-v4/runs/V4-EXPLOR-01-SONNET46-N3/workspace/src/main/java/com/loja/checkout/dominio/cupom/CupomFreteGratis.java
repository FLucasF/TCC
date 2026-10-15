package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return frete;
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
        // sem restrição
    }
}
