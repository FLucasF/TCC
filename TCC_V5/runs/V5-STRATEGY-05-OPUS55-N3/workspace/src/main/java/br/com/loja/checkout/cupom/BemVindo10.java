package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class BemVindo10 implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
    }
}
