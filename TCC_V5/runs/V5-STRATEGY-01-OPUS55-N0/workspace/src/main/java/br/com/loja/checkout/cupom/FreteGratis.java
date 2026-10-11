package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O desconto é igual ao valor do frete. */
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
