package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** R$ 18,00 fixos, no mesmo dia, para pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public BigDecimal calcularFrete(DadosEntrega dados) {
        return TAXA_FIXA;
    }

    @Override
    public boolean atende(DadosEntrega dados) {
        return dados.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
