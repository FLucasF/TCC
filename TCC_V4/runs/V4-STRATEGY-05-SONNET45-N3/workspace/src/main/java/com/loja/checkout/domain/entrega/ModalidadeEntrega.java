package com.loja.checkout.domain.entrega;

import com.loja.checkout.model.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    boolean estaDisponivel(List<ItemPedido> itens);
    BigDecimal calcularFrete(List<ItemPedido> itens);
    int getPrazoEntregaDias();
}
