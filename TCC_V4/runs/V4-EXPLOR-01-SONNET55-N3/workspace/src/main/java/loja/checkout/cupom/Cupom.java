package loja.checkout.cupom;

import java.math.BigDecimal;

import loja.checkout.comum.Codigo;
import loja.checkout.comum.Compra;

public interface Cupom extends Codigo {
    BigDecimal desconto(Compra compra, BigDecimal frete);

    default boolean aplicavel(Compra compra) {
        return true;
    }
}
