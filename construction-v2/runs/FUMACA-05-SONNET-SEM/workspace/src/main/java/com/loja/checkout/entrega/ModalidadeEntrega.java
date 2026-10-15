package com.loja.checkout.entrega;

import com.loja.checkout.domain.PedidoContext;

import java.math.BigDecimal;

/**
 * Estrategia de calculo de frete. Cada opcao de entrega da loja (economica, expressa,
 * retirada, motoboy, e as que forem chegando de novas transportadoras) implementa esta
 * interface e e registrada como um bean Spring, sem precisar tocar no restante do calculo.
 */
public interface ModalidadeEntrega {

    String getCodigo();

    boolean disponivel(PedidoContext pedido);

    BigDecimal calcularFrete(PedidoContext pedido);

    int getPrazoDias();
}
