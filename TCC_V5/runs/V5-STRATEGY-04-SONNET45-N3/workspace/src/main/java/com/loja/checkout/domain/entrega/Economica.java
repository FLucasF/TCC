package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public boolean estaDisponivel(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        BigDecimal frete = TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoKg));
        return Arredondamento.arredondar(frete);
    }

    @Override
    public int obterPrazo() {
        return 7;
    }
}
