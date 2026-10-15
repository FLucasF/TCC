package com.loja.domain.modalidade;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean isDisponivel(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pesoTotal.compareTo(LIMITE_PESO) <= 0;
    }
}
