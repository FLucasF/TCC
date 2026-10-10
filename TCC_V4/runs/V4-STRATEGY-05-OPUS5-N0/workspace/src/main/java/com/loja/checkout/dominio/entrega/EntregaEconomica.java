package com.loja.checkout.dominio.entrega;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
public class EntregaEconomica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Dinheiro.valor(FIXO.add(POR_KG.multiply(pesoTotalKg, Dinheiro.CALCULO)));
    }
}
