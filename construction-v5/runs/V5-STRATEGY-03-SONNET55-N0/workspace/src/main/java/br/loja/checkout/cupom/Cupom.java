package br.loja.checkout.cupom;

import br.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Um cupom de desconto. Para criar um novo, basta implementar e anotar com @Component. */
public interface Cupom {
    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto já arredondado; {@code frete} é o frete que será cobrado no resumo. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
