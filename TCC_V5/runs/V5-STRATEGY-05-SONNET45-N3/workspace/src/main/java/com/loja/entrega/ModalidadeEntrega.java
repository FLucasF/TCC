package com.loja.entrega;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    int obterPrazo();
    boolean estaDisponivel(List<ItemCarrinho> itens);
}
