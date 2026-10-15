package com.loja.pedidos.entrega;

/** Uma opção de entrega. Para criar uma nova opção basta registrar outro bean que implemente esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    /** Calcula frete e prazo, ou lança MODALIDADE_INDISPONIVEL / TRANSPORTADORA_INDISPONIVEL. */
    Cotacao cotar(PedidoEntrega pedido);

    /** Se a opção cobra frete (a retirada na loja não). */
    default boolean temFrete() {
        return true;
    }

    /** Se benefícios do clube que zeram o frete valem para esta opção. */
    default boolean permiteIsencaoFrete() {
        return true;
    }
}
