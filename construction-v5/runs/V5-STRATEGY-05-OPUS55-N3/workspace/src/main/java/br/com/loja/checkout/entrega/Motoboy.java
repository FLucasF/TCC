package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(new BigDecimal("5")) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
