package com.loja.resumo.cupom;

import com.loja.resumo.Contexto;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Contexto contexto) {
        return contexto.frete();
    }
}
