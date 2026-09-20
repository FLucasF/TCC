package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 10% de desconto no valor dos produtos. */
@Component
public class CupomBemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.subtotalProdutos().multiply(PERCENTUAL);
    }
}
