package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;

public class EntregaMotoboy implements CalculadoraEntrega {
    private static final double PESO_MAXIMO = 5.0;

    @Override
    public BigDecimal calcular(List<Item> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public boolean podeEntregar(List<Item> itens) {
        double peso = calcularPeso(itens);
        return peso <= PESO_MAXIMO;
    }

    private double calcularPeso(List<Item> itens) {
        return itens.stream().mapToDouble(item -> item.pesoKg * item.quantidade).sum();
    }
}
