package com.loja.resumo.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelPrata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.02");
    }
}
