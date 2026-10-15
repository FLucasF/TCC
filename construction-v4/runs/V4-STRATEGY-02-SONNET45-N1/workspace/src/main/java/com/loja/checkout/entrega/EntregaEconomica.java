package com.loja.checkout.entrega;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class EntregaEconomica implements Entrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotal))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        return true;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
