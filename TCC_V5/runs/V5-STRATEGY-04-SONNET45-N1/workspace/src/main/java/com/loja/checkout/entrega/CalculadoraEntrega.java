package com.loja.checkout.entrega;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public interface CalculadoraEntrega {
    BigDecimal calcularFrete(Pedido pedido);
    int getPrazoEntregaDias();
    boolean podeAtender(Pedido pedido);
}
