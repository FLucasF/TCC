package loja.checkout.cupom;

import loja.checkout.Codificado;
import loja.checkout.Pedido;

import java.math.BigDecimal;

/** Cada cupom mora na sua própria classe. O frete já considera os benefícios do clube. */
public interface Cupom extends Codificado {

    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
