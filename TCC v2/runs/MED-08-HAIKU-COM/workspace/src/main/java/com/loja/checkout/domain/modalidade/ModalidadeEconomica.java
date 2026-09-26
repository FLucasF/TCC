package com.loja.checkout.domain.modalidade;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;

public class ModalidadeEconomica implements Modalidade {
    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");
    private static final int PRAZO = 7;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal frete = TAXA_BASE.add(pesoTotalKg.multiply(TAXA_POR_KG));
        return Arredondamento.arredondarParaCentavos(frete);
    }

    @Override
    public int obterPrazoEntrega() {
        return PRAZO;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public String obterCodigo() {
        return "ECONOMICA";
    }
}
