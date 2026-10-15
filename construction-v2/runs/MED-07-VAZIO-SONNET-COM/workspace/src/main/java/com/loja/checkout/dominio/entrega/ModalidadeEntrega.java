package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(Pedido pedido);

    BigDecimal calcularFrete(Pedido pedido);

    int prazoEntregaDias();
}
