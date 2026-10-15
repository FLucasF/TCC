package com.loja.checkout.service.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements CupomStrategy {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto contexto) {
        return contexto.freteCalculado();
    }
}
