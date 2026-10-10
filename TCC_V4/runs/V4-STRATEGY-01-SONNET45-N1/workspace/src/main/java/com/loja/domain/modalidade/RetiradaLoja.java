package com.loja.domain.modalidade;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("0.00");
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean isDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
