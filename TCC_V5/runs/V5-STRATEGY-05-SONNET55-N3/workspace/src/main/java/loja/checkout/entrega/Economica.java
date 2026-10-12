package loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
public class Economica implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(BASE.add(POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
