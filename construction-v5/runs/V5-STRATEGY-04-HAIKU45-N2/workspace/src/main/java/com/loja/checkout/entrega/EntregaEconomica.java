package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;
import com.loja.checkout.util.Arredonda;

public class EntregaEconomica implements CalculadoraEntrega {
    @Override
    public BigDecimal calcular(List<Item> itens) {
        double peso = calcularPeso(itens);
        return Arredonda.arredonda(12.00 + 2.00 * peso);
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public boolean podeEntregar(List<Item> itens) {
        return true;
    }

    private double calcularPeso(List<Item> itens) {
        return itens.stream().mapToDouble(item -> item.pesoKg * item.quantidade).sum();
    }
}
