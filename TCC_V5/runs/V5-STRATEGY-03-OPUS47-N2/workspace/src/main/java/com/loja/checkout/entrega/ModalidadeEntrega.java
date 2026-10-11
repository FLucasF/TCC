package com.loja.checkout.entrega;

import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    void validar(Pedido pedido);
    BigDecimal frete(Pedido pedido);
    int prazoDias();
}
