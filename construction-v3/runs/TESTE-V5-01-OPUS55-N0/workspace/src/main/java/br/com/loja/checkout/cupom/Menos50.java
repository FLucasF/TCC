package br.com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto para compras a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return DESCONTO;
    }
}
