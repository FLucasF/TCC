package com.loja.checkout.entrega;

import com.loja.checkout.model.Pedido;
import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(Pedido pedido);
    Integer prazoEntregaDias();
    void validarDisponibilidade(Pedido pedido);
}
