package com.loja.checkout.domain.modalidade;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal VALOR_BASE = new BigDecimal("12.00");
    private static final BigDecimal VALOR_POR_KG = new BigDecimal("2.00");
    private static final int PRAZO_DIAS = 7;

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
