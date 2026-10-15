package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** 10% de desconto no valor dos produtos. */
@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public Dinheiro desconto(ContextoCupom contexto) {
        return contexto.subtotalProdutos().vezes(TAXA);
    }
}
