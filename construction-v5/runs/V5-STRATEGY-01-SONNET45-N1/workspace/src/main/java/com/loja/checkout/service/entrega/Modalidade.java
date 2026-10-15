package com.loja.checkout.service.entrega;

import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;

public interface Modalidade {
    String getCodigo();
    boolean aceita(Pedido pedido);
    BigDecimal calcularFrete(Pedido pedido);
    int getPrazoDias();
}
