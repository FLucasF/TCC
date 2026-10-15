package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import java.util.List;

public class FreteMotoboy implements FreteCalculador {
    private static final double PESO_MAXIMO = 5.0;

    @Override
    public double calcularFrete(List<Item> itens) {
        return 18.0;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean ehDisponivel(List<Item> itens) {
        double pesoTotal = itens.stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
        return pesoTotal <= PESO_MAXIMO;
    }
}
