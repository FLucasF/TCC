package com.loja.checkout.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ModalidadeEntrega;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EconomicaOpcaoEntrega implements OpcaoEntrega {

    private static final BigDecimal CUSTO_BASE = new BigDecimal("12.00");
    private static final BigDecimal CUSTO_POR_KG = new BigDecimal("2.00");

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.ECONOMICA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Dinheiro.arredondar(CUSTO_BASE.add(CUSTO_POR_KG.multiply(pesoTotalKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
