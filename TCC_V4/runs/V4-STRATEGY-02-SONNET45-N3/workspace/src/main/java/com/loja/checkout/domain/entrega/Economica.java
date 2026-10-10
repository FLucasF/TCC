package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        BigDecimal fixo = Dinheiro.de(12.00);
        BigDecimal porPeso = Dinheiro.de(2.00 * pesoTotalKg);
        return Dinheiro.arredondar(fixo.add(porPeso));
    }

    @Override
    public int obterPrazo() {
        return 7;
    }

    @Override
    public boolean aceitaPedido(double pesoTotalKg) {
        return true;
    }
}
