package br.com.loja.checkout.entrega;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para incluir uma transportadora nova, crie um {@code @Component}
 * que implemente esta interface: ela passa a ser aceita pelo código retornado em {@link #codigo()}.
 */
public interface ModalidadeEntrega extends Codificado {

    /** Frete cobrado pela opção, já arredondado para centavos. */
    BigDecimal calcularFrete(Carrinho carrinho);

    int prazoDias();

    /** Se a opção consegue levar este pedido (limite de peso, por exemplo). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
