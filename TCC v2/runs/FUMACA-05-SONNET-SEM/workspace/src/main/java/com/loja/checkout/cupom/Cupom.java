package com.loja.checkout.cupom;

import com.loja.checkout.domain.PedidoContext;

import java.math.BigDecimal;

/**
 * Estrategia de calculo de desconto de cupom. Cada cupom criado pelo marketing
 * implementa esta interface e e registrado como um bean Spring.
 */
public interface Cupom {

    String getCodigo();

    boolean aplicavel(PedidoContext pedido);

    /**
     * @param frete frete ja calculado para o pedido, usado por cupons como o FRETEGRATIS.
     */
    BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete);
}
