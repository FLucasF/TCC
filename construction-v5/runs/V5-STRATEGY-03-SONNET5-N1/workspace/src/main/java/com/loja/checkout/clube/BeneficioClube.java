package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import java.math.BigDecimal;

/**
 * Cada nivel do clube tem seu proprio conjunto de vantagens, e a loja esta
 * estudando criar mais niveis. Um nivel novo so precisa de uma implementacao
 * nova desta interface.
 */
public interface BeneficioClube {

    NivelClube nivel();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
