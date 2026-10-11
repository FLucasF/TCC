package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = BigDecimal.valueOf(12.00);
    private static final BigDecimal POR_KG = BigDecimal.valueOf(2.00);

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(double pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(BigDecimal.valueOf(pesoTotalKg))));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
