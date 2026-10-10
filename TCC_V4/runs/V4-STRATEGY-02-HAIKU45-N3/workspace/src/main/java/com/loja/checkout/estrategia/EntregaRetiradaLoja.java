package com.loja.checkout.estrategia;

public class EntregaRetiradaLoja implements EstrategiaEntrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 0.0;
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean disponivel(double pesoKg) {
        return true;
    }
}
