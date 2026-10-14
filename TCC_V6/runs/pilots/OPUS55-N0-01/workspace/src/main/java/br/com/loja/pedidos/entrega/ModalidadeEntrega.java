package br.com.loja.pedidos.entrega;

/**
 * Uma opção de entrega. Para criar uma nova opção, basta criar um novo componente que implemente esta interface.
 */
public interface ModalidadeEntrega {

    /** Código usado pelo site em {@code modalidadeEntrega}. */
    String codigo();

    /**
     * Calcula frete e prazo. Recusa com {@code MODALIDADE_INDISPONIVEL} quando a opção não atende o pedido e com
     * {@code TRANSPORTADORA_INDISPONIVEL} quando depende de um parceiro que está fora do ar.
     */
    CotacaoEntrega cotar(ContextoEntrega contexto);

    /** Se a opção tem frete (a retirada na loja não tem). */
    default boolean temFrete() {
        return true;
    }

    /** Se o frete desta opção pode ser isentado pelo clube da loja. */
    default boolean isentavelPeloClube() {
        return true;
    }
}
