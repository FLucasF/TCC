package com.loja.checkout.resumo.entrega;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** R$ 18,00 no mesmo dia, só para pedidos de até 5 kg. */
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(VALOR);
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
