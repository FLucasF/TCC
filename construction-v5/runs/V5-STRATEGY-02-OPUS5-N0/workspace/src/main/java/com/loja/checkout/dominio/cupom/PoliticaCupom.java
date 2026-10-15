package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Um cupom da loja. Promocao nova e implementacao nova anotada com @Component.
 * So um cupom e aceito por pedido.
 */
public interface PoliticaCupom extends Identificavel {

    /**
     * Desconto do cupom, arredondado para centavos. Recebe tambem o frete
     * do pedido (ja com as isencoes do clube aplicadas).
     */
    BigDecimal desconto(BaseCalculo base, BigDecimal frete);

    /** Se o pedido cumpre a condicao do cupom. */
    default boolean aplicavel(BaseCalculo base, BigDecimal frete) {
        return true;
    }
}
