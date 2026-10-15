package com.loja.checkout.clube;

import com.loja.checkout.dominio.Registro;
import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel tem seu conjunto de vantagens,
 * e cada conjunto mora na sua propria classe; o nivel declara so o que ganha.
 */
public interface NivelClube extends Registro.Identificado {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que o nivel deixa o cliente pagar. */
    default BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    /** Se o pedido vai com brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
