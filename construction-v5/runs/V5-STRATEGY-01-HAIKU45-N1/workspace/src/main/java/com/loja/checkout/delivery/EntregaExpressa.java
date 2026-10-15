package com.loja.checkout.delivery;

public class EntregaExpressa implements ModalidadeEntrega {
    @Override
    public double calcularFrete(double pesoTotalKg) {
        return 25.00 + (4.50 * pesoTotalKg);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "EXPRESSA";
    }
}
