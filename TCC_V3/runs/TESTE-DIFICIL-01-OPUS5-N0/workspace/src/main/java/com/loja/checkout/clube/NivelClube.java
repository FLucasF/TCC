package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel diz quais vantagens o cliente tem.
 * Para criar um nivel novo, basta um bean novo implementando esta interface.
 */
public interface NivelClube extends Identificavel {

    /** Percentual do valor dos produtos devolvido em credito para a proxima compra. */
    BigDecimal percentualCredito();

    /** Se o nivel nao paga frete. */
    boolean freteGratis();

    /** Ate quantas parcelas o cartao fica sem juros para este nivel. */
    int parcelasSemJuros();

    /** Se o pedido leva brinde, dado o valor dos produtos. */
    boolean temBrinde(BigDecimal subtotalProdutos);
}
