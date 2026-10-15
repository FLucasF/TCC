package com.loja.checkout.modalidade;

import com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Modalidade {
    String codigo();
    boolean atende(Pedido pedido);
    BigDecimal frete(Pedido pedido);
    int prazoDias();
}
