package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja e as vantagens dele. Para criar um nivel novo basta uma
 * classe nova implementando esta interface, anotada com @Component.
 */
public interface NivelClube {

    /** Codigo que o site manda em "nivelClube". */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos (sem desconto e sem frete). */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Se o nivel nao paga frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido vem com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
