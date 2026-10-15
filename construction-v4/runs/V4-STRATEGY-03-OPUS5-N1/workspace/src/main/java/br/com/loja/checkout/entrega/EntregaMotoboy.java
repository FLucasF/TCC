package br.com.loja.checkout.entrega;

import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal TARIFA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(TARIFA);
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
