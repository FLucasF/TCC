package br.com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente não paga o frete: o desconto é igual ao valor do frete. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
