package com.loja.checkout.coupon;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratis implements Coupon {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto contexto) {
        return contexto.frete();
    }
}
