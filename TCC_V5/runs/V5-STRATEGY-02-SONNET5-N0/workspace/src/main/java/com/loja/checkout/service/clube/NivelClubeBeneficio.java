package com.loja.checkout.service.clube;

import java.math.BigDecimal;

/**
 * As vantagens de um nivel do clube. Novos niveis entram implementando
 * esta interface e registrando um componente Spring.
 */
public interface NivelClubeBeneficio {

    String getCodigo();

    boolean freteGratis();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
