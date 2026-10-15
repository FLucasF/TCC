package br.com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O frete continua aparecendo no resumo; o desconto do cupom é igual a ele. */
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
