package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return new BigDecimal("50.00");
    }
}
