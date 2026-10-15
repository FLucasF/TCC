package com.loja.checkout.entrega;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements Entrega {
    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
