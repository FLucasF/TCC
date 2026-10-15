package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja e suas vantagens. Para criar um nível novo, basta criar um
 * {@code @Component} que implemente esta interface.
 */
public interface NivelClube {

    String codigo();

    /** Percentual dos produtos devolvido em crédito para a próxima compra (ex.: 0.02 para 2%). */
    default BigDecimal taxaCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o cliente deste nível não paga frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }

    /** Crédito para a próxima compra: sobre o valor dos produtos, sem desconto e sem frete. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaCredito());
    }
}
