package com.loja.checkout.delivery;

public class EntregaRetiraLoja implements ModalidadeEntrega {
    @Override
    public double calcularFrete(double pesoTotalKg) {
        return 0.00;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public String getNome() {
        return "RETIRADA_LOJA";
    }
}
