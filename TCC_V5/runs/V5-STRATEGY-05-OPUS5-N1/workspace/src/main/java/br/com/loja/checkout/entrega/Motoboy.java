package br.com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
