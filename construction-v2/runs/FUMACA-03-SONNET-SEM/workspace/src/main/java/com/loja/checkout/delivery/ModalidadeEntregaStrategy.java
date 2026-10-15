package com.loja.checkout.delivery;

import com.loja.checkout.PedidoContext;

import java.math.BigDecimal;

/**
 * Implemente esta interface e anote com {@code @Component} para cadastrar uma nova
 * opção de entrega sem precisar tocar em mais nenhum código.
 */
public interface ModalidadeEntregaStrategy {

    String getCodigo();

    int getPrazoDias();

    boolean disponivelPara(PedidoContext pedido);

    BigDecimal calcularFrete(PedidoContext pedido);
}
