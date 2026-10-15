package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 12,00 + R$ 2,00 por kg, 7 dias. */
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
    public BigDecimal frete(Carrinho carrinho) {
        return FIXO.add(POR_KG.multiply(carrinho.pesoTotalKg()));
    }
}
