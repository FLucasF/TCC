package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    String getCodigo();
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    Integer getPrazoEntregaDias();
    boolean aceitaPedido(List<ItemCarrinho> itens);
}
