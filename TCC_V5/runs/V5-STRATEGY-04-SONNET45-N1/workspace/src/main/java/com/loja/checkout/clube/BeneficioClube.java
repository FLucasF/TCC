package com.loja.checkout.clube;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public interface BeneficioClube {
    BigDecimal calcularCredito(Pedido pedido);
    boolean temFreteGratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
