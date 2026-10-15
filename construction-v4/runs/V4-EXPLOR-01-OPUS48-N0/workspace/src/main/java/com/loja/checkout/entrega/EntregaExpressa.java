package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg, prazo de 2 dias. */
@Component
public class EntregaExpressa implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal custoFrete(BigDecimal pesoKg) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pesoKg)));
    }
}
