package com.loja.checkout.clube;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Um nível do clube e suas vantagens. Nível novo = classe nova com @Component.
 */
public interface NivelClube extends Codificavel {

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
