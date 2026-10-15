package com.loja.checkout.entrega;

import com.loja.checkout.dominio.PedidoContext;

/**
 * Uma forma de entrega disponivel na loja. Para adicionar uma nova transportadora,
 * basta criar uma nova implementacao anotada com {@code @Component}: ela e
 * detectada automaticamente pelo {@link ModalidadeEntregaRegistry}, sem alterar
 * o restante do calculo.
 */
public interface ModalidadeEntrega {

    /** Codigo usado no campo "modalidadeEntrega" da requisicao, ex.: "EXPRESSA". */
    String codigo();

    /** Se essa modalidade atende o pedido informado (ex.: limite de peso). */
    boolean disponivel(PedidoContext pedido);

    /** Calcula o frete e o prazo de entrega para o pedido. */
    CalculoFrete calcular(PedidoContext pedido);
}
