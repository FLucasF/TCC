package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(Pedido pedido, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete);
}
