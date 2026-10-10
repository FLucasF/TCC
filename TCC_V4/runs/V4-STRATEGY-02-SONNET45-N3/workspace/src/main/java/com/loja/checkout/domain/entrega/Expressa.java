package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        BigDecimal fixo = Dinheiro.de(25.00);
        BigDecimal porPeso = Dinheiro.de(4.50 * pesoTotalKg);
        return Dinheiro.arredondar(fixo.add(porPeso));
    }

    @Override
    public int obterPrazo() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(double pesoTotalKg) {
        return true;
    }
}
