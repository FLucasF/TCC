package com.loja.checkout.estrategia.modalidade;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    private static final BigDecimal TAXA_BASE = Moeda.de(25.00);
    private static final BigDecimal TAXA_POR_KG = Moeda.de(4.50);
    private static final int PRAZO = 2;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return Moeda.arredondar(TAXA_BASE.add(TAXA_POR_KG.multiply(pesoTotal)));
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
