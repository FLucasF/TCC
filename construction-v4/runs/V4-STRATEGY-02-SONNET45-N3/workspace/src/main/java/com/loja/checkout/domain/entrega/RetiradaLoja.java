package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return Dinheiro.de(0.00);
    }

    @Override
    public int obterPrazo() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(double pesoTotalKg) {
        return true;
    }
}
