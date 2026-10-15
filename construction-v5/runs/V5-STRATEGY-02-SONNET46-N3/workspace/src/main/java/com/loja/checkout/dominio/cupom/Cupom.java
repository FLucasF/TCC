package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ResumoRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean eAplicavel(BigDecimal subtotalProdutos);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ResumoRequest.Item> itens);
}
