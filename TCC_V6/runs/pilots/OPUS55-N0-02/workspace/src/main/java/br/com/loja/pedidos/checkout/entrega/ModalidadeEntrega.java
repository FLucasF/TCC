package br.com.loja.pedidos.checkout.entrega;

/**
 * Uma opção de entrega. Para criar uma opção nova, basta um novo componente do Spring que
 * implemente esta interface: ele passa a ser aceito no campo {@code modalidadeEntrega}.
 */
public interface ModalidadeEntrega {

    /** Código usado no campo {@code modalidadeEntrega}. */
    String codigo();

    /**
     * Calcula o frete e o prazo do pedido.
     *
     * @throws br.com.loja.pedidos.comum.ErroNegocio {@code MODALIDADE_INDISPONIVEL} se a opção não atende o
     *         pedido, ou {@code TRANSPORTADORA_INDISPONIVEL} se depende de uma transportadora fora do ar
     */
    CotacaoEntrega cotar(PedidoParaEntrega pedido);

    /** Se vantagens do clube (como o frete grátis do OURO) valem nesta opção. */
    default boolean aceitaFreteGratisDoClube() {
        return true;
    }
}
