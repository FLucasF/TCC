package com.loja.checkout.estrategia.modalidade;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    private static final int PRAZO = 1;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return Moeda.de(0.00);
    }

    @Override
    public int obterPrazo() {
        return PRAZO;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
