package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    default void validarAplicavel(ContextoPedido contexto) {
    }

    BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete);
}
