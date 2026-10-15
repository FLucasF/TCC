package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class EntregaExpressa implements EstrategiaEntrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return Arredondamento.arredondar(25.00 + (4.50 * pesoKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean disponivel(double pesoKg) {
        return true;
    }
}
