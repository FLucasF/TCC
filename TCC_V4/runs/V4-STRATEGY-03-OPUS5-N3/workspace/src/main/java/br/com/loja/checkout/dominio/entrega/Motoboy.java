package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final Dinheiro FIXO = Dinheiro.de("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public Dinheiro frete(BigDecimal pesoKg) {
        return FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
