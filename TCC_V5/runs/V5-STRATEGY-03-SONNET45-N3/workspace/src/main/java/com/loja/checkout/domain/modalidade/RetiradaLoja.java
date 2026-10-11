package com.loja.checkout.domain.modalidade;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int obterPrazoDias() {
        return 1;
    }
}
