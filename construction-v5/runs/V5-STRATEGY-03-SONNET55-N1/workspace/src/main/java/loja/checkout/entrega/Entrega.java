package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Identificavel;

public interface Entrega extends Identificavel {

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
