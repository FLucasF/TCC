package com.loja.checkout.clube;

import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(Pedido pedido);
    BigDecimal ajustarFrete(BigDecimal frete);
    Boolean temBrinde(Pedido pedido);
}
