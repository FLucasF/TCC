package com.loja.checkout.entrega;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class EntregaMotoboy implements Entrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5.00");

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return TAXA_FIXA.setScale(2);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return pesoTotal.compareTo(LIMITE_PESO_KG) <= 0;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
