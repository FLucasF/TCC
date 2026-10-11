package com.loja.checkout.domain.modalidade;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    private static final BigDecimal VALOR_BASE = new BigDecimal("25.00");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("4.50");
    private static final int PRAZO_DIAS = 2;

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularValor(BigDecimal pesoTotalKg) {
        BigDecimal valor = VALOR_BASE.add(VALOR_POR_KG.multiply(pesoTotalKg));
        return Arredondamento.arredondar(valor);
    }

    @Override
    public int obterPrazoDias() {
        return PRAZO_DIAS;
    }
}
