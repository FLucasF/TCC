package com.loja.checkout.estrategia.modalidade;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal TAXA_BASE = Moeda.de(12.00);
    private static final BigDecimal TAXA_POR_KG = Moeda.de(2.00);
    private static final int PRAZO = 7;

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
