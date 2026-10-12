package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        return contexto.frete();
    }
}
