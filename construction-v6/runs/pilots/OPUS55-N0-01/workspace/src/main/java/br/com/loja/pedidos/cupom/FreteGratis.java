package br.com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/** O desconto é o próprio frete. Não vale para opções sem frete, como a retirada na loja. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        if (!contexto.modalidade().temFrete()) {
            throw Cupom.naoAplicavel();
        }
        return contexto.frete();
    }
}
