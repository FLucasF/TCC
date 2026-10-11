package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente nao paga o frete: o frete continua aparecendo no resumo e o
 * desconto do cupom fica igual ao valor dele.
 */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
