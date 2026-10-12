package com.loja.entrega;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int obterPrazo() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
