package com.loja.checkout.entrega;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Expressa implements Entrega {
    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
