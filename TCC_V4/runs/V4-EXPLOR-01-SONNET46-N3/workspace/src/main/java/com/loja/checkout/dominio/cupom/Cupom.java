package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
    void validarAplicabilidade(BigDecimal subtotalProdutos);
}
