package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Cada cupom novo entra como uma implementacao,
 * com sua conta de desconto e sua condicao.
 */
public interface Cupom extends Codificado {

    BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Se o pedido cumpre a condicao do cupom. */
    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }
}
