package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return Dinheiro.de(18.00);
    }

    @Override
    public int obterPrazo() {
        return 0;
    }

    @Override
    public boolean aceitaPedido(double pesoTotalKg) {
        return pesoTotalKg <= 5.0;
    }
}
