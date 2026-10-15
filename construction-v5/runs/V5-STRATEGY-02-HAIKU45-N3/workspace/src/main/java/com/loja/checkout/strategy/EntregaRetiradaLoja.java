package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class EntregaRetiradaLoja implements Entrega {

    @Override
    public BigDecimal calcularCusto(Double pesoKg) {
        return ArredondadorMeio.zero();
    }

    @Override
    public Integer prazo() {
        return 1;
    }

    @Override
    public boolean podeAtender(Double pesoKg) {
        return true;
    }

    @Override
    public String nome() {
        return "RETIRADA_LOJA";
    }

}
