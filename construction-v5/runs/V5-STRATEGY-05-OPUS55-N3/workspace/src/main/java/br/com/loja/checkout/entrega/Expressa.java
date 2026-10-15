package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Expressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
