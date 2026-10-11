package com.loja.checkout.delivery;

public class EntregaEconomica implements ModalidadeEntrega {
    @Override
    public double calcularFrete(double pesoTotalKg) {
        return 12.00 + (2.00 * pesoTotalKg);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "ECONOMICA";
    }
}
