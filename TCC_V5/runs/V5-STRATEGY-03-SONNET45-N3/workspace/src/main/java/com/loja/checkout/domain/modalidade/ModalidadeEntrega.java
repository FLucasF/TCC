package com.loja.checkout.domain.modalidade;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface ModalidadeEntrega {
    boolean aceitaPedido(List<ItemCarrinho> itens);
    BigDecimal calcularFrete(List<ItemCarrinho> itens);
    int obterPrazoDias();
}
