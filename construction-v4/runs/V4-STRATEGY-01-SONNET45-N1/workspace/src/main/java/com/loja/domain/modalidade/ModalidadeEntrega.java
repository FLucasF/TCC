package com.loja.domain.modalidade;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    int getPrazoEntregaDias();
    boolean isDisponivel(List<ItemCarrinho> itens);
}
