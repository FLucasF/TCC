package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;
import com.loja.checkout.domain.Resumo;

import java.math.BigDecimal;

public interface Cupom {
    String getCodigo();
    boolean podeAplicar(Pedido pedido, BigDecimal frete);
    BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete);
}
