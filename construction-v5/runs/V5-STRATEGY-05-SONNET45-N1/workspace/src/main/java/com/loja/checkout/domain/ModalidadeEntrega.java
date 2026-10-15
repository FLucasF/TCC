package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.ItemCarrinho;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    int prazoEntregaDias();
    boolean aceitaPedido(List<ItemCarrinho> itens);
}
