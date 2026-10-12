package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/** Um nível do clube da loja, com seu conjunto de vantagens. */
public interface NivelClube {

    String codigo();

    /** Frete que o cliente deste nível paga, a partir do frete da entrega. */
    BigDecimal frete(BigDecimal freteEntrega);

    /** Crédito para a próxima compra. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
