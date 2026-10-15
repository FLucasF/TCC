package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Ponto de extensao: cada nova transportadora/opcao de entrega vira uma
 * implementacao desta interface, sem alterar o restante do sistema.
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    boolean disponivelPara(PedidoContexto contexto);

    BigDecimal calcularFrete(PedidoContexto contexto);
}
