package com.loja.checkout.clube;

import com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/** As vantagens de um nível do clube da loja. */
public interface NivelClube extends Codificado {

    /** Percentual dos produtos devolvido em crédito para a próxima compra (ex.: 2 para 2%). */
    BigDecimal percentualCredito();

    /** Frete cobrado do cliente, a partir do frete da modalidade de entrega. */
    BigDecimal frete(BigDecimal freteModalidade);

    boolean daBrinde(BigDecimal subtotalProdutos);
}
