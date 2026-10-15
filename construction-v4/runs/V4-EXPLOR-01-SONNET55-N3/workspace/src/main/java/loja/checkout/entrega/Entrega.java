package loja.checkout.entrega;

import java.math.BigDecimal;

import loja.checkout.comum.Codigo;
import loja.checkout.comum.Compra;

public interface Entrega extends Codigo {
    int prazoDias();

    BigDecimal frete(Compra compra);

    default boolean atende(Compra compra) {
        return true;
    }
}
