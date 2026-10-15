package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar uma promoção nova, crie um {@code @Component}
 * que implemente esta interface.
 */
public interface Cupom extends Codificado {

    /**
     * Desconto concedido, já arredondado para centavos.
     *
     * @param frete o frete que vai aparecer no resumo (já considerando o clube)
     */
    BigDecimal calcularDesconto(Carrinho carrinho, BigDecimal frete);

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }
}
