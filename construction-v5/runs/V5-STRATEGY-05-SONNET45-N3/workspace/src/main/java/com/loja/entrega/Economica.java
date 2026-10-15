package com.loja.entrega;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Economica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BigDecimal("12.00")
            .add(new BigDecimal("2.00").multiply(pesoTotal))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int obterPrazo() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
