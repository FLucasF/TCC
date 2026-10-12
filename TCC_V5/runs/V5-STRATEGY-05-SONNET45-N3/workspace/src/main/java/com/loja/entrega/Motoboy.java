package com.loja.entrega;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("18.00").setScale(2);
    }

    @Override
    public int obterPrazo() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
    }
}
