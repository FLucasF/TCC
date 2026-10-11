package com.loja.checkout.service.cupom;

import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;

public interface Cupom {
    String getCodigo();
    boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete);
    BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete);
}
