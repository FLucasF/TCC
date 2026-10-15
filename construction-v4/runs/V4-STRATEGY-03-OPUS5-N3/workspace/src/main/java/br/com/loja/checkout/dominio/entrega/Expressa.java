package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

@Component
public class Expressa implements ModalidadeEntrega {

    private static final Dinheiro FIXO = Dinheiro.de("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public Dinheiro frete(BigDecimal pesoKg) {
        return FIXO.mais(Dinheiro.de(POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
