package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal BASE = BigDecimal.valueOf(25.00);
    private static final BigDecimal POR_KG = BigDecimal.valueOf(4.50);

    @Override
    public String codigo() {
        return "EXPRESSA";
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
        return 2;
    }
}
