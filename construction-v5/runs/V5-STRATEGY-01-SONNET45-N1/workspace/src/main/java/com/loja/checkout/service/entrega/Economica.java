package com.loja.checkout.service.entrega;

import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Economica implements Modalidade {
    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean aceita(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal pesoTotal = pedido.itens().stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TAXA_BASE.add(TAXA_POR_KG.multiply(pesoTotal))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }
}
