package com.loja.checkout.clube;

import com.loja.checkout.service.PedidoContext;

import java.math.BigDecimal;

public interface NivelClube {

    String getCodigo();

    BigDecimal calcularCredito(PedidoContext pedido);

    boolean freteGratis();

    boolean temDireitoABrinde(PedidoContext pedido);
}
