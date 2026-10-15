package com.loja.checkout.estrategia.modalidade;

import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal TAXA_FIXA = Moeda.de(18.00);
    private static final int PRAZO = 0;
    private static final BigDecimal PESO_MAXIMO = Moeda.de(5.00);

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA;
    }

    @Override
    public int obterPrazo() {
        return PRAZO;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
