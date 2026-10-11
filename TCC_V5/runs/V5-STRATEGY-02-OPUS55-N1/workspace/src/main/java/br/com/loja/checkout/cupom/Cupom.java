package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Cada cupom define sua condição e como calcula o desconto. */
public interface Cupom {

    String codigo();

    /** @param frete o frete que o cliente pagaria, já considerando o clube */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }
}
