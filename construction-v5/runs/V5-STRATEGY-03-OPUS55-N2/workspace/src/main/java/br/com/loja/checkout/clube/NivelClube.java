package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface NivelClube {

    String codigo();

    /** Frete que o cliente deste nível paga, dado o frete da modalidade escolhida. */
    BigDecimal freteCobrado(BigDecimal freteModalidade);

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
